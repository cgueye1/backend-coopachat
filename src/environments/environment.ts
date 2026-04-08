export const environment = {
  production: false,

  /** Backend distant (tests depuis le front local). */
  apiUrl: 'http://86.106.181.31:8083/api',
  imageServerUrl: 'http://86.106.181.31:8083/api',

  // Backend sur la même machine (décommenter si besoin) :
  // apiUrl: 'http://localhost:8083/api',
  // imageServerUrl: 'http://localhost:8083/api',
  //
  // Prod / préprod HTTPS (site front en HTTPS ) :
  // apiUrl: 'https://api.coopachat.innovimpactdev.cloud/api',
  // imageServerUrl: 'https://api.coopachat.innovimpactdev.cloud/api',
};
