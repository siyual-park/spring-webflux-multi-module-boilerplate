import { Options } from 'k6/options';

import { ClientGateway } from './gateway';
import client from './client';
import { dummyCreateClientRequest } from "./dummy";

export let options: Options = {
  vus: 50,
  duration: '10s'
};

const clientGateway = new ClientGateway(client);

export default () => {
  clientGateway.create(dummyCreateClientRequest());
};
