import { useRef, type KeyboardEvent, type ClipboardEvent, type ChangeEvent } from 'react';

export interface OtpInputProps {
  value: string;
  onChange: (value: string) => void;
  onComplete?: (value: string) => void;
  disabled?: boolean;
  autoFocus?: boolean;
  error?: string;
  id?: string;
}

export function OtpInput({
  value = '',
  onChange,
  onComplete,
  disabled = false,
  autoFocus = false,
  error,
  id = 'otp-input',
}: OtpInputProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const digits = value.slice(0, 6).split('');
  const activeIndex = Math.min(digits.length, 5);

  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    const raw = e.target.value;
    // Strictly filter out all non-digit characters
    const sanitized = raw.replace(/\D/g, '').slice(0, 6);
    if (sanitized !== value) {
      onChange(sanitized);
      if (sanitized.length === 6 && onComplete) {
        onComplete(sanitized);
      }
    }
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    // If a non-numeric key other than allowed control keys is pressed, prevent default
    if (
      !/^\d$/.test(e.key) &&
      !['Backspace', 'Delete', 'ArrowLeft', 'ArrowRight', 'Tab', 'Home', 'End'].includes(e.key) &&
      !e.ctrlKey &&
      !e.metaKey
    ) {
      e.preventDefault();
      return;
    }

    if (e.key === 'Backspace') {
      if (value.length > 0) {
        onChange(value.slice(0, -1));
        e.preventDefault();
      }
    }
  };

  const handlePaste = (e: ClipboardEvent<HTMLInputElement>) => {
    e.preventDefault();
    const pasted = e.clipboardData.getData('text/plain') || '';
    const digitsOnly = pasted.replace(/\D/g, '').slice(0, 6);
    if (digitsOnly !== value) {
      onChange(digitsOnly);
      if (digitsOnly.length === 6 && onComplete) {
        onComplete(digitsOnly);
      }
    }
  };

  const focusInput = () => {
    if (!disabled && inputRef.current) {
      inputRef.current.focus();
    }
  };

  return (
    <div className="otp-container">
      <div
        className={`otp-boxes ${error ? 'has-error' : ''} ${disabled ? 'is-disabled' : ''}`}
        onClick={focusInput}
        role="group"
        aria-label="6-digit authenticator code"
      >
        {[0, 1, 2, 3, 4, 5].map((index) => {
          const char = digits[index] ?? '';
          const isCurrent = !disabled && index === activeIndex;
          const isFilled = Boolean(char);

          return (
            <div
              key={index}
              data-testid="otp-box"
              className={`otp-box ${isCurrent ? 'is-active' : ''} ${isFilled ? 'is-filled' : ''}`}
            >
              {char || <span className="otp-placeholder">_</span>}
            </div>
          );
        })}
      </div>

      {/* Accessible underlying input */}
      <input
        ref={inputRef}
        id={id}
        type="text"
        inputMode="numeric"
        pattern="[0-9]*"
        autoComplete="one-time-code"
        maxLength={6}
        value={value}
        onChange={handleChange}
        onKeyDown={handleKeyDown}
        onPaste={handlePaste}
        disabled={disabled}
        autoFocus={autoFocus}
        className="otp-hidden-input"
        aria-label="Enter 6-digit authenticator code"
        aria-invalid={Boolean(error)}
      />

      {error ? <span className="badge danger" style={{ marginTop: '6px' }}>{error}</span> : null}
    </div>
  );
}
