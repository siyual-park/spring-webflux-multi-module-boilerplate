import { Options } from 'k6/options';

import { AuthGateway, GatewayAuthorization } from './gateway';
import client from './client';
import matrixType from './matrix-type';

export const options: Options = {
  stages: [
    { duration: '2m', target: 4000 },
    { duration: '3h56m', target: 4000 },
    { duration: '2m', target: 0 },
  ],
};

matrixType(options, ['POST_token', 'GET_principal']);

const authGateway = new AuthGateway();

const gatewayAuthorization = new GatewayAuthorization({
  grantType: 'client_credentials',
  clientId: client.id,
  clientSecret: client.secret,
});

export default () => {
  authGateway.read(gatewayAuthorization.getAuthorization());
};
