import { render, screen, fireEvent } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { OtpInput } from './OtpInput';

describe('OtpInput Component', () => {
  it('renders 6 visual digit slots and 1 accessible numeric input', () => {
    render(<OtpInput value="" onChange={vi.fn()} />);

    const boxes = screen.getAllByTestId('otp-box');
    expect(boxes).toHaveLength(6);

    const input = screen.getByRole('textbox');
    expect(input).toHaveAttribute('inputMode', 'numeric');
    expect(input).toHaveAttribute('pattern', '[0-9]*');
    expect(input).toHaveAttribute('maxLength', '6');
    expect(input).toHaveAttribute('autoComplete', 'one-time-code');
  });

  it('rejects alphabetic characters and symbols on typing', () => {
    const handleChange = vi.fn();
    render(<OtpInput value="" onChange={handleChange} />);
    const input = screen.getByRole('textbox');

    // Attempt to type letters or symbols
    fireEvent.change(input, { target: { value: 'abc!@#' } });
    expect(handleChange).not.toHaveBeenCalled();
  });

  it('accepts digits and updates visual boxes', () => {
    const handleChange = vi.fn();
    const { rerender } = render(<OtpInput value="12" onChange={handleChange} />);

    const boxes = screen.getAllByTestId('otp-box');
    expect(boxes[0]).toHaveTextContent('1');
    expect(boxes[1]).toHaveTextContent('2');
    expect(boxes[2]).toHaveTextContent('_');

    const input = screen.getByRole('textbox');
    fireEvent.change(input, { target: { value: '123' } });
    expect(handleChange).toHaveBeenCalledWith('123');

    rerender(<OtpInput value="123" onChange={handleChange} />);
    const updatedBoxes = screen.getAllByTestId('otp-box');
    expect(updatedBoxes[2]).toHaveTextContent('3');
  });

  it('handles pasting a 6-digit numeric string and invokes onComplete', () => {
    const handleChange = vi.fn();
    const handleComplete = vi.fn();
    render(<OtpInput value="" onChange={handleChange} onComplete={handleComplete} />);
    const input = screen.getByRole('textbox');

    fireEvent.paste(input, {
      clipboardData: {
        getData: () => '849201',
      },
    });

    expect(handleChange).toHaveBeenCalledWith('849201');
    expect(handleComplete).toHaveBeenCalledWith('849201');
  });

  it('strips non-digits from pasted string', () => {
    const handleChange = vi.fn();
    render(<OtpInput value="" onChange={handleChange} />);
    const input = screen.getByRole('textbox');

    fireEvent.paste(input, {
      clipboardData: {
        getData: () => ' 84-92 01 ',
      },
    });

    expect(handleChange).toHaveBeenCalledWith('849201');
  });
});
