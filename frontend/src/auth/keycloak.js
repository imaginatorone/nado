import Keycloak from 'keycloak-js';

let keycloakInstance = null;

export function createKeycloak(config) {
  if (keycloakInstance) return keycloakInstance;

  keycloakInstance = new Keycloak({
    url: config.keycloakUrl,
    realm: config.realm,
    clientId: config.clientId,
  });

  return keycloakInstance;
}

export function getKeycloak() {
  return keycloakInstance;
}
