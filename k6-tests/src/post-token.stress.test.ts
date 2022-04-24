import { Options } from 'k6/options';

import { AuthGateway, UserGateway } from './gateway';
import { TokenInfo, UserInfo } from './response';
import { dummyCreateUserRequest } from './dummy';

import matrixScenarios from './matrix-scenarios';
import client from './client';

export const options: Options = {
  scenarios: {
    clientCredentials: {
      gracefulStop: '0s',
      exec: 'clientCredentials',
      executor: 'ramping-arrival-rate',
      preAllocatedVUs: 10000,
      stages: [
        { duration: '2m', target: 4000 },
        { duration: '5m', target: 4000 },
        { duration: '2m', target: 6000 },
        { duration: '5m', target: 6000 },
        { duration: '2m', target: 8000 },
        { duration: '5m', target: 8000 },
        { duration: '2m', target: 10000 },
        { duration: '5m', target: 10000 },
        { duration: '10m', target: 0 },
      ],
    },
    password: {
      gracefulStop: '0s',
      startTime: '38m',
      exec: 'password',
      executor: 'ramping-arrival-rate',
      preAllocatedVUs: 10000,
      stages: [
        { duration: '2m', target: 4000 },
        { duration: '5m', target: 4000 },
        { duration: '2m', target: 6000 },
        { duration: '5m', target: 6000 },
        { duration: '2m', target: 8000 },
        { duration: '5m', target: 8000 },
        { duration: '2m', target: 10000 },
        { duration: '5m', target: 10000 },
        { duration: '10m', target: 0 },
      ],
    },
    refreshToken: {
      gracefulStop: '0s',
      startTime: '76m',
      exec: 'refreshToken',
      executor: 'ramping-arrival-rate',
      preAllocatedVUs: 10000,
      stages: [
        { duration: '2m', target: 4000 },
        { duration: '5m', target: 4000 },
        { duration: '2m', target: 6000 },
        { duration: '5m', target: 6000 },
        { duration: '2m', target: 8000 },
        { duration: '5m', target: 8000 },
        { duration: '2m', target: 10000 },
        { duration: '5m', target: 10000 },
        { duration: '10m', target: 0 },
      ],
    },
  },
};

matrixScenarios(options);

const authGateway = new AuthGateway();
const userGateway = new UserGateway({
  grantType: 'client_credentials',
  clientId: client.id,
  clientSecret: client.secret,
});

export function setup() {
  const token = authGateway.createToken({
    grantType: 'client_credentials',
    clientId: client.id,
    clientSecret: client.secret,
  });
  const createUserRequest = dummyCreateUserRequest();
  const user = userGateway.create(createUserRequest);

  return {
    token,
    user: {
      ...user,
      ...createUserRequest,
    },
  };
}

export function clientCredentials() {
  authGateway.createToken({
    grantType: 'client_credentials',
    clientId: client.id,
    clientSecret: client.secret,
  });
}

export function password({ user }: { user: UserInfo & { password: string } }) {
  authGateway.createToken({
    grantType: 'password',
    clientId: client.id,
    clientSecret: client.secret,
    username: user.name,
    password: user.password,
  });
}

export function refreshToken({ token }: { token: TokenInfo }) {
  const { refreshToken = '' } = token;
  authGateway.createToken({
    grantType: 'refresh_token',
    clientId: client.id,
    clientSecret: client.secret,
    refreshToken: refreshToken,
  });
}

