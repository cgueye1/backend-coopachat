/**
 * Messages réseau / navigateur pour l’utilisateur final (évite « Failed to fetch », etc.).
 */

export const HTTP_NETWORK_USER_MESSAGE_FR =
  'Connexion au serveur impossible. Vérifiez votre connexion internet, que l’application est à jour, et que le service est bien démarré (adresse de l’API). Réessayez dans quelques instants.';

/** Phrases typiques du navigateur / fetch (hors message Angular « Http failure … : 401 »). */
const FETCH_OR_NETWORK_MARKERS = [
  'failed to fetch',
  'networkerror',
  'network request failed',
  'load failed',
  'econnrefused',
  'err_connection',
  'net::err_',
];

function isAngularZeroResponseMessage(text: string): boolean {
  const lower = text.toLowerCase();
  return lower.includes('http failure response') && (lower.includes(': 0 ') || lower.includes('0 unknown'));
}

/** Texte générique d’Angular HttpClient sur erreur HTTP — pas lisible pour l’utilisateur. */
function isAngularHttpClientNoise(text: string): boolean {
  return text.toLowerCase().includes('http failure response for');
}

function isTechnicalClientMessage(text: string): boolean {
  const lower = text.trim().toLowerCase();
  if (!lower) {
    return false;
  }
  if (FETCH_OR_NETWORK_MARKERS.some((m) => lower.includes(m))) {
    return true;
  }
  return isAngularZeroResponseMessage(lower);
}

/** Erreur réseau, CORS, serveur arrêté, etc. (Angular utilise souvent status 0). */
export function isNetworkOrFetchError(error: unknown): boolean {
  const e = error as { status?: number; message?: string; name?: string };
  if (e?.status === 0) {
    return true;
  }
  const raw = [e?.message, e?.name].filter((x): x is string => typeof x === 'string').join(' ').toLowerCase();
  if (!raw) {
    return false;
  }
  if (FETCH_OR_NETWORK_MARKERS.some((m) => raw.includes(m))) {
    return true;
  }
  return isAngularZeroResponseMessage(raw);
}

/**
 * Message à afficher : priorité au détail API, sinon erreurs réseau, sinon fallback.
 */
export function getUserFacingHttpErrorMessage(error: unknown, fallback: string): string {
  if (isNetworkOrFetchError(error)) {
    return HTTP_NETWORK_USER_MESSAGE_FR;
  }

  const e = error as { error?: unknown; message?: string };
  let text: string | null = null;
  const body = e?.error;

  if (body && typeof body === 'object' && 'message' in body) {
    const m = (body as { message?: unknown }).message;
    if (typeof m === 'string' && m.trim()) {
      text = m.trim();
    }
  }
  if (!text && typeof body === 'string' && body.trim()) {
    try {
      const parsed = JSON.parse(body) as { message?: string };
      if (typeof parsed?.message === 'string' && parsed.message.trim()) {
        text = parsed.message.trim();
      }
    } catch {
      text = body.trim();
    }
  }
  if (!text && typeof e?.message === 'string' && e.message.trim()) {
    text = e.message.trim();
  }

  if (text && isAngularHttpClientNoise(text) && !isAngularZeroResponseMessage(text)) {
    text = null;
  }

  if (text && isTechnicalClientMessage(text)) {
    return HTTP_NETWORK_USER_MESSAGE_FR;
  }
  if (text) {
    return text;
  }
  return fallback;
}
