import { Options } from 'k6/options';

import { UserGateway } from './gateway';
import { dummyCreateUserRequest } from './dummy';

import client from './client';
import matrixType from './matrix-type';

export const options: Options = {
  stages: [
    { duration: '2m', target: 6000 },
    { duration: '3h56m', target: 6000 },
    { duration: '2m', target: 0 },
  ],
};

matrixType(options, ['POST_users']);

const userGateway = new UserGateway({
  grantType: 'client_credentials',
  clientId: client.id,
  clientSecret: client.secret,
});

export default () => {
  userGateway.create(dummyCreateUserRequest());
};
