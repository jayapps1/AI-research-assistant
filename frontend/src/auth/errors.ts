export class TotpChallengeRequired extends Error {
  challengeId: string;
  expiresIn: number;

  constructor(challengeId: string, expiresIn: number) {
    super('Authenticator verification is required.');
    this.name = 'TotpChallengeRequired';
    this.challengeId = challengeId;
    this.expiresIn = expiresIn;
  }
}
