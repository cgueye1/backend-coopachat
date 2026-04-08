/** Modèle d’affichage pour une ligne utilisateur / modal détail (admin + mon compte). */
export interface UserDisplay {
  id: number;
  ref: string;
  name: string;
  initials: string;
  email: string;
  role: string;
  createdAt: string;
  status: 'Actif' | 'Inactif';
  phone?: string;
  profilePhotoUrl?: string | null;
}

export function formatUserDate(v: string | { day?: number; month?: number; year?: number } | unknown): string {
  if (typeof v === 'string') {
    const d = new Date(v);
    if (!isNaN(d.getTime())) {
      return d.toLocaleString('fr-FR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    }
    return v;
  }
  return '';
}
