export class TotpChallengeRequired extends Error {
  challengeId: string;
  expiresIn: number;
  email?: string;

  constructor(challengeId: string, expiresIn: number, email?: string) {
    super('Authenticator verification is required.');
    this.name = 'TotpChallengeRequired';
    this.challengeId = challengeId;
    this.expiresIn = expiresIn;
    this.email = email;
  }
}
