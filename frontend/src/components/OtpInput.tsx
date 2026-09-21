import { useRef, type KeyboardEvent, type ClipboardEvent } from 'react';

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
  const inputsRef = useRef<(HTMLInputElement | null)[]>([]);
  const digits = Array.from({ length: 6 }, (_, i) => value[i] ?? '');

  const handleInputChange = (index: number, rawValue: string) => {
    const clean = rawValue.replace(/\D/g, '');
    if (!clean) {
      const newDigits = [...digits];
      newDigits[index] = '';
      onChange(newDigits.join('').trimEnd());
      return;
    }

    if (clean.length > 1) {
      // Multiple digits typed or pasted into single box
      const newDigits = [...digits];
      for (let j = 0; j < clean.length && index + j < 6; j++) {
        newDigits[index + j] = clean[j];
      }
      const newCode = newDigits.join('').slice(0, 6);
      onChange(newCode);
      const nextFocus = Math.min(index + clean.length, 5);
      inputsRef.current[nextFocus]?.focus();
      if (newCode.length === 6 && onComplete) {
        onComplete(newCode);
      }
      return;
    }

    // Single digit entered
    const newDigits = [...digits];
    newDigits[index] = clean;
    const newCode = newDigits.join('').slice(0, 6);
    onChange(newCode);

    if (index < 5) {
      inputsRef.current[index + 1]?.focus();
    }
    if (newCode.length === 6 && onComplete) {
      onComplete(newCode);
    }
  };

  const handleKeyDown = (index: number, e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace') {
      if (!digits[index] && index > 0) {
        e.preventDefault();
        const newDigits = [...digits];
        newDigits[index - 1] = '';
        onChange(newDigits.join('').trimEnd());
        inputsRef.current[index - 1]?.focus();
      }
      return;
    }

    if (e.key === 'ArrowLeft') {
      if (index > 0) {
        e.preventDefault();
        inputsRef.current[index - 1]?.focus();
      }
      return;
    }

    if (e.key === 'ArrowRight') {
      if (index < 5) {
        e.preventDefault();
        inputsRef.current[index + 1]?.focus();
      }
      return;
    }

    if (e.key === 'Enter') {
      if (value.length === 6 && onComplete) {
        onComplete(value);
      }
    }
  };

  const handlePaste = (e: ClipboardEvent<HTMLInputElement>) => {
    e.preventDefault();
    const pasted = e.clipboardData.getData('text/plain') || '';
    const clean = pasted.replace(/\D/g, '').slice(0, 6);
    if (clean) {
      onChange(clean);
      if (clean.length === 6) {
        inputsRef.current[5]?.focus();
        if (onComplete) {
          onComplete(clean);
        }
      } else {
        inputsRef.current[clean.length]?.focus();
      }
    }
  };

  return (
    <div className="otp-container">
      <div
        className={`otp-boxes ${error ? 'has-error' : ''} ${disabled ? 'is-disabled' : ''}`}
        role="group"
        aria-label="One-time verification code"
      >
        {[0, 1, 2, 3, 4, 5].map((index) => {
          return (
            <input
              key={index}
              ref={(el) => {
                inputsRef.current[index] = el;
              }}
              data-testid="otp-box"
              id={index === 0 ? id : `${id}-${index}`}
              type="text"
              inputMode="numeric"
              pattern="[0-9]*"
              autoComplete={index === 0 ? 'one-time-code' : 'off'}
              maxLength={1}
              value={digits[index]}
              disabled={disabled}
              autoFocus={autoFocus && index === 0}
              className={`otp-box ${digits[index] ? 'is-filled' : ''}`}
              aria-label={`Digit ${index + 1} of 6`}
              aria-invalid={Boolean(error)}
              onChange={(e) => handleInputChange(index, e.target.value)}
              onKeyDown={(e) => handleKeyDown(index, e)}
              onPaste={handlePaste}
              onFocus={(e) => e.target.select()}
            />
          );
        })}
      </div>

      {error ? <span className="badge danger" style={{ marginTop: '6px' }}>{error}</span> : null}
    </div>
  );
}
