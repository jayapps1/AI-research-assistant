import { render, screen, fireEvent } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { OtpInput } from './OtpInput';

describe('OtpInput Component', () => {
  it('renders 6 individual numeric input boxes', () => {
    render(<OtpInput value="" onChange={vi.fn()} />);

    const boxes = screen.getAllByTestId('otp-box');
    expect(boxes).toHaveLength(6);

    boxes.forEach((box, idx) => {
      expect(box).toHaveAttribute('inputMode', 'numeric');
      expect(box).toHaveAttribute('pattern', '[0-9]*');
      expect(box).toHaveAttribute('maxLength', '1');
      expect(box).toHaveAttribute('aria-label', `Digit ${idx + 1} of 6`);
    });

    expect(boxes[0]).toHaveAttribute('autoComplete', 'one-time-code');
  });

  it('rejects non-numeric characters on typing', () => {
    const handleChange = vi.fn();
    render(<OtpInput value="" onChange={handleChange} />);
    const boxes = screen.getAllByTestId('otp-box');

    fireEvent.change(boxes[0], { target: { value: 'a' } });
    expect(handleChange).not.toHaveBeenCalledWith('a');
  });

  it('accepts digits and updates individual boxes', () => {
    const handleChange = vi.fn();
    const { rerender } = render(<OtpInput value="12" onChange={handleChange} />);

    const boxes = screen.getAllByTestId('otp-box');
    expect(boxes[0]).toHaveValue('1');
    expect(boxes[1]).toHaveValue('2');
    expect(boxes[2]).toHaveValue('');

    fireEvent.change(boxes[2], { target: { value: '3' } });
    expect(handleChange).toHaveBeenCalledWith('123');

    rerender(<OtpInput value="123" onChange={handleChange} />);
    const updatedBoxes = screen.getAllByTestId('otp-box');
    expect(updatedBoxes[2]).toHaveValue('3');
  });

  it('handles pasting a 6-digit numeric string and invokes onComplete', () => {
    const handleChange = vi.fn();
    const handleComplete = vi.fn();
    render(<OtpInput value="" onChange={handleChange} onComplete={handleComplete} />);
    const boxes = screen.getAllByTestId('otp-box');

    fireEvent.paste(boxes[0], {
      clipboardData: {
        getData: () => '849201',
      },
    });

    expect(handleChange).toHaveBeenCalledWith('849201');
    expect(handleComplete).toHaveBeenCalledWith('849201');
  });

  it('strips non-digits and spaces from pasted string', () => {
    const handleChange = vi.fn();
    render(<OtpInput value="" onChange={handleChange} />);
    const boxes = screen.getAllByTestId('otp-box');

    fireEvent.paste(boxes[0], {
      clipboardData: {
        getData: () => ' 84-92 01 ',
      },
    });

    expect(handleChange).toHaveBeenCalledWith('849201');
  });

  it('handles backspace on empty box moving to previous box', () => {
    const handleChange = vi.fn();
    render(<OtpInput value="12" onChange={handleChange} />);
    const boxes = screen.getAllByTestId('otp-box');

    // Press Backspace on box 2 (which is empty)
    fireEvent.keyDown(boxes[2], { key: 'Backspace' });
    expect(handleChange).toHaveBeenCalledWith('1');
  });

  it('handles arrow key navigation between boxes', () => {
    render(<OtpInput value="123" onChange={vi.fn()} />);
    const boxes = screen.getAllByTestId('otp-box');

    boxes[1].focus();
    fireEvent.keyDown(boxes[1], { key: 'ArrowLeft' });
    fireEvent.keyDown(boxes[1], { key: 'ArrowRight' });
  });
});
