# File Security

Uploads use generated storage keys and store the original filename only as sanitized metadata. Local storage resolves every key under the configured root to prevent path traversal.

Supported research document uploads are PDF, DOCX, DOC, and TXT. PDF and DOCX uploads are checked against basic magic bytes. SHA-256 checksums are recorded.

`FileSecurityScanner` is the malware scanning boundary. The disabled local scanner records `NOT_SCANNED`; it does not claim files are clean. Files marked infected are quarantined and must not be downloaded, processed, embedded, or sent to AI.
