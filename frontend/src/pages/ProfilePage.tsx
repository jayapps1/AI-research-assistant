import { useState, useRef, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../auth/AuthProvider';
import { profileApi } from '../api/endpoints';
import { Avatar } from '../components/Avatar';
import { Badge, Button, Card } from '../components/ui';
import { Camera, Trash2, CheckCircle2, AlertCircle, RefreshCw, X } from 'lucide-react';
import type { UserProfileResponse } from '../types/api';

const MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

export function ProfilePage() {
  const auth = useAuth();
  const queryClient = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const { data: profile, isLoading } = useQuery<UserProfileResponse>({
    queryKey: ['profile'],
    queryFn: profileApi.getProfile,
    staleTime: 60_000,
  });

  const [firstName, setFirstName] = useState(() => profile?.firstName || auth.user?.firstName || '');
  const [lastName, setLastName] = useState(() => profile?.lastName || auth.user?.lastName || '');
  const [phoneNumber, setPhoneNumber] = useState(() => profile?.phoneNumber || auth.user?.phoneNumber || '');
  const [locale, setLocale] = useState(() => profile?.locale || auth.user?.locale || 'en');
  const [prevProfile, setPrevProfile] = useState(profile);

  if (profile !== prevProfile) {
    setPrevProfile(profile);
    if (profile) {
      setFirstName(profile.firstName || '');
      setLastName(profile.lastName || '');
      setPhoneNumber(profile.phoneNumber || '');
      setLocale(profile.locale || 'en');
    } else if (auth.user) {
      setFirstName(auth.user.firstName || '');
      setLastName(auth.user.lastName || '');
      setPhoneNumber(auth.user.phoneNumber || '');
      setLocale(auth.user.locale || 'en');
    }
  }

  const updateProfileMutation = useMutation({
    mutationFn: (data: { firstName?: string; lastName?: string; phoneNumber?: string; locale?: string }) =>
      profileApi.updateProfile(data),
    onSuccess: (updatedProfile) => {
      queryClient.setQueryData(['profile'], updatedProfile);
      queryClient.invalidateQueries({ queryKey: ['profile'] });
      queryClient.invalidateQueries({ queryKey: ['me'] });
      queryClient.invalidateQueries({ queryKey: ['topbar-user'] });
      queryClient.invalidateQueries({ queryKey: ['settings-profile'] });
      if (auth.user) {
        auth.updateUser({
          firstName: updatedProfile.firstName ?? undefined,
          lastName: updatedProfile.lastName ?? undefined,
          fullName: updatedProfile.displayName || `${updatedProfile.firstName || ''} ${updatedProfile.lastName || ''}`.trim(),
          phoneNumber: updatedProfile.phoneNumber ?? undefined,
          locale: updatedProfile.locale ?? undefined,
        });
      }
      setPhoneNumber(updatedProfile.phoneNumber || '');
      setSuccessMsg('Profile updated successfully.');
      setErrorMsg(null);
    },
    onError: (err: any) => {
      const msg = err.response?.data?.validationErrors?.phoneNumber ||
        err.response?.data?.message ||
        err.message ||
        'Failed to update profile.';
      setErrorMsg(msg);
      setSuccessMsg(null);
    },
  });

  const handleProfileSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    setSuccessMsg(null);
    updateProfileMutation.mutate({
      firstName: firstName.trim(),
      lastName: lastName.trim(),
      phoneNumber: phoneNumber.trim(),
      locale: locale.trim(),
    });
  };

  useEffect(() => {
    return () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
    };
  }, [previewUrl]);

  const uploadMutation = useMutation({
    mutationFn: (file: File) => profileApi.uploadImage(file),
    onSuccess: (updatedProfile) => {
      queryClient.setQueryData(['profile'], updatedProfile);
      queryClient.invalidateQueries({ queryKey: ['profile'] });
      if (auth.user) {
        const cacheBuster = Date.now();
        auth.updateUser({
          avatarUrl: `${updatedProfile.avatarUrl}?t=${cacheBuster}`,
          profileImage: updatedProfile.profileImage,
        });
      }
      setSuccessMsg('Profile picture updated successfully.');
      setErrorMsg(null);
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
      setPreviewUrl(null);
      setSelectedFile(null);
      if (fileInputRef.current) fileInputRef.current.value = '';
    },
    onError: (err: Error) => {
      setErrorMsg(err.message || 'Failed to upload profile picture. Please verify the file is a valid image.');
      setSuccessMsg(null);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => profileApi.deleteImage(),
    onSuccess: (updatedProfile) => {
      queryClient.setQueryData(['profile'], updatedProfile);
      queryClient.invalidateQueries({ queryKey: ['profile'] });
      if (auth.user) {
        auth.updateUser({
          avatarUrl: null,
          profileImage: null,
        });
      }
      setSuccessMsg('Profile picture removed.');
      setErrorMsg(null);
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
      setPreviewUrl(null);
      setSelectedFile(null);
      if (fileInputRef.current) fileInputRef.current.value = '';
    },
    onError: (err: Error) => {
      setErrorMsg(err.message || 'Failed to remove profile picture.');
      setSuccessMsg(null);
    },
  });

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setErrorMsg(null);
    setSuccessMsg(null);
    const file = e.target.files?.[0];
    if (!file) return;

    if (!ALLOWED_TYPES.includes(file.type)) {
      setErrorMsg('Invalid file format. Please upload a JPEG, PNG, or WebP image.');
      if (fileInputRef.current) fileInputRef.current.value = '';
      return;
    }

    if (file.size > MAX_FILE_SIZE) {
      setErrorMsg(`File size (${(file.size / (1024 * 1024)).toFixed(1)}MB) exceeds the 5MB limit.`);
      if (fileInputRef.current) fileInputRef.current.value = '';
      return;
    }

    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
    }

    const objectUrl = URL.createObjectURL(file);
    setSelectedFile(file);
    setPreviewUrl(objectUrl);
  };

  const handleCancelPreview = () => {
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
    }
    setPreviewUrl(null);
    setSelectedFile(null);
    setErrorMsg(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const handleSavePhoto = () => {
    if (!selectedFile) return;
    uploadMutation.mutate(selectedFile);
  };

  const handleRemovePhoto = () => {
    if (window.confirm('Are you sure you want to remove your profile picture?')) {
      deleteMutation.mutate();
    }
  };

  const displayName = profile?.fullName || auth.user?.fullName ||
    (`${auth.user?.firstName ?? ''} ${auth.user?.lastName ?? ''}`.trim() || 'Research Assistant User');

  const currentAvatarSrc = previewUrl || profile?.avatarUrl || auth.user?.avatarUrl;
  const hasActivePhoto = Boolean(profile?.profileImage && profile.profileImage.status === 'ACTIVE') || Boolean(profile?.avatarUrl);

  return (
    <section className="page profile-page">
      <div className="page-header" style={{ marginBottom: '24px' }}>
        <div>
          <h1 className="page-title" style={{ margin: 0 }}>My Profile</h1>
          <p className="muted" style={{ margin: '4px 0 0' }}>
            Manage your personal identity, avatar image, and account settings.
          </p>
        </div>
      </div>

      {successMsg && (
        <div className="alert success" style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px' }}>
          <CheckCircle2 size={18} />
          <span>{successMsg}</span>
        </div>
      )}

      {errorMsg && (
        <div className="alert danger" style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px' }}>
          <AlertCircle size={18} />
          <span>{errorMsg}</span>
        </div>
      )}

      <div className="grid cols-2" style={{ gap: '24px', alignItems: 'start' }}>
        {/* Left Card: Avatar & Photo Management */}
        <Card className="profile-card">
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
            <h2 style={{ fontSize: '1.2rem', margin: 0 }}>Profile Photo</h2>
            {isLoading && <RefreshCw size={16} className="spin muted" />}
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center', padding: '16px 0' }}>
            <div style={{ position: 'relative', display: 'inline-block' }}>
              <Avatar
                src={currentAvatarSrc}
                name={displayName}
                email={auth.user?.email}
                size={110}
                border
              />
              {previewUrl && (
                <span
                  style={{
                    position: 'absolute',
                    bottom: 0,
                    right: 0,
                    background: 'var(--brand, #155eef)',
                    color: '#fff',
                    borderRadius: '50%',
                    padding: '4px',
                    boxShadow: '0 2px 4px rgba(0,0,0,0.2)',
                  }}
                  title="New photo selected (unsaved)"
                >
                  <Camera size={14} />
                </span>
              )}
            </div>

            <div style={{ marginTop: '16px' }}>
              <h3 style={{ margin: 0, fontSize: '1.1rem' }}>{displayName}</h3>
              <p className="muted" style={{ margin: '4px 0 0', fontSize: '0.9rem' }}>{auth.user?.email}</p>
            </div>

            {/* Hidden native file input */}
            <input
              type="file"
              ref={fileInputRef}
              accept="image/jpeg,image/png,image/webp"
              style={{ display: 'none' }}
              onChange={handleFileChange}
            />

            {/* Actions */}
            <div style={{ display: 'flex', gap: '10px', marginTop: '20px', flexWrap: 'wrap', justifyContent: 'center' }}>
              {!selectedFile ? (
                <>
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={() => fileInputRef.current?.click()}
                    style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
                  >
                    <Camera size={15} />
                    {hasActivePhoto ? 'Change Photo' : 'Upload Photo'}
                  </Button>
                  {hasActivePhoto && (
                    <Button
                      type="button"
                      variant="danger"
                      onClick={handleRemovePhoto}
                      disabled={deleteMutation.isPending}
                      style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
                    >
                      <Trash2 size={15} />
                      {deleteMutation.isPending ? 'Removing...' : 'Remove'}
                    </Button>
                  )}
                </>
              ) : (
                <>
                  <Button
                    type="button"
                    variant="primary"
                    onClick={handleSavePhoto}
                    disabled={uploadMutation.isPending}
                    style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
                  >
                    <CheckCircle2 size={15} />
                    {uploadMutation.isPending ? 'Uploading...' : 'Save Photo'}
                  </Button>
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={handleCancelPreview}
                    disabled={uploadMutation.isPending}
                    style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
                  >
                    <X size={15} />
                    Cancel
                  </Button>
                </>
              )}
            </div>

            <p className="muted" style={{ fontSize: '0.78rem', marginTop: '16px', maxWidth: '320px' }}>
              Accepted formats: JPEG, PNG, or WebP. Maximum file size: 5MB. Square or portrait photos work best.
            </p>
          </div>

          {profile?.profileImage && (
            <div style={{ borderTop: '1px solid var(--line)', paddingTop: '14px', marginTop: '8px', fontSize: '0.85rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
                <span className="muted">Image Status</span>
                <Badge tone="success">{profile.profileImage.status}</Badge>
              </div>
              {profile.profileImage.width && profile.profileImage.height && (
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
                  <span className="muted">Dimensions</span>
                  <span>{profile.profileImage.width} × {profile.profileImage.height} px</span>
                </div>
              )}
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
                <span className="muted">File Size</span>
                <span>{(profile.profileImage.fileSizeBytes / 1024).toFixed(1)} KB</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span className="muted">Format</span>
                <span>{profile.profileImage.contentType}</span>
              </div>
            </div>
          )}
        </Card>

        {/* Right Card: Account Details & Navigation */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <Card>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
              <h2 style={{ fontSize: '1.2rem', margin: 0 }}>Account Information</h2>
              {updateProfileMutation.isPending && <RefreshCw size={16} className="spin muted" />}
            </div>

            <form onSubmit={handleProfileSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div className="grid cols-2" style={{ gap: '12px' }}>
                <div>
                  <label htmlFor="profile-first-name" style={{ display: 'block', fontSize: '0.85rem', fontWeight: 500, marginBottom: '6px' }}>
                    First Name
                  </label>
                  <input
                    id="profile-first-name"
                    type="text"
                    className="input"
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    placeholder="First name"
                    maxLength={100}
                    style={{ width: '100%' }}
                  />
                </div>
                <div>
                  <label htmlFor="profile-last-name" style={{ display: 'block', fontSize: '0.85rem', fontWeight: 500, marginBottom: '6px' }}>
                    Last Name
                  </label>
                  <input
                    id="profile-last-name"
                    type="text"
                    className="input"
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                    placeholder="Last name"
                    maxLength={100}
                    style={{ width: '100%' }}
                  />
                </div>
              </div>

              <div>
                <label htmlFor="profile-email" style={{ display: 'block', fontSize: '0.85rem', fontWeight: 500, marginBottom: '6px' }}>
                  Email Address
                </label>
                <input
                  id="profile-email"
                  type="email"
                  className="input"
                  value={auth.user?.email || ''}
                  disabled
                  style={{ width: '100%', opacity: 0.7, cursor: 'not-allowed' }}
                />
                <p className="muted" style={{ fontSize: '0.75rem', margin: '4px 0 0' }}>
                  Email changes require a separate verified email-change flow.
                </p>
              </div>

              <div>
                <label htmlFor="profile-phone" style={{ display: 'block', fontSize: '0.85rem', fontWeight: 500, marginBottom: '6px' }}>
                  Phone Number
                </label>
                <input
                  id="profile-phone"
                  type="tel"
                  className="input"
                  value={phoneNumber}
                  onChange={(e) => setPhoneNumber(e.target.value)}
                  placeholder="0542011738 or +233542011738"
                  maxLength={40}
                  style={{ width: '100%' }}
                />
                <p className="muted" style={{ fontSize: '0.78rem', margin: '4px 0 0', color: 'var(--brand, #155eef)' }}>
                  Ghana numbers can be entered as 0542011738 or +233542011738.
                </p>
              </div>

              <div className="grid cols-2" style={{ gap: '12px' }}>
                <div>
                  <label htmlFor="profile-locale" style={{ display: 'block', fontSize: '0.85rem', fontWeight: 500, marginBottom: '6px' }}>
                    Preferred Locale
                  </label>
                  <select
                    id="profile-locale"
                    className="input select"
                    value={locale}
                    onChange={(e) => setLocale(e.target.value)}
                    style={{ width: '100%' }}
                  >
                    <option value="en">English (en)</option>
                    <option value="en-GH">English - Ghana (en-GH)</option>
                    <option value="en-US">English - US (en-US)</option>
                    <option value="en-GB">English - UK (en-GB)</option>
                  </select>
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 500, marginBottom: '6px' }}>
                    Roles & Access
                  </label>
                  <div style={{ display: 'flex', gap: '6px', alignItems: 'center', minHeight: '38px', flexWrap: 'wrap' }}>
                    {auth.hasCapability('admin') ? (
                      <Badge tone="info">SYSTEM_ADMIN</Badge>
                    ) : (
                      <Badge tone="success">RESEARCHER</Badge>
                    )}
                    <Badge>{auth.user?.status ?? 'ACTIVE'}</Badge>
                  </div>
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '8px' }}>
                <Button
                  id="profile-save-button"
                  type="submit"
                  variant="primary"
                  disabled={updateProfileMutation.isPending}
                  style={{ minWidth: '130px' }}
                >
                  {updateProfileMutation.isPending ? 'Saving...' : 'Save Changes'}
                </Button>
              </div>
            </form>
          </Card>

          <Card>
            <h2 style={{ fontSize: '1.2rem', margin: '0 0 12px' }}>Security & Preferences</h2>
            <p className="muted" style={{ fontSize: '0.88rem', margin: '0 0 16px' }}>
              Configure two-factor authentication, change password, or adjust notification channels.
            </p>
            <div style={{ display: 'flex', gap: '10px' }}>
              <Button
                variant="secondary"
                onClick={() => window.location.assign('/app/settings/security')}
              >
                Security Settings
              </Button>
              <Button
                variant="secondary"
                onClick={() => window.location.assign('/app/settings/notifications')}
              >
                Notifications
              </Button>
            </div>
          </Card>
        </div>
      </div>
    </section>
  );
}
