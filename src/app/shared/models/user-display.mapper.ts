import { UserDetailsDTO } from '../services/admin.service';
import { UserDisplay, formatUserDate } from './user-display.model';

function formatDetailCreatedAt(v: string | unknown): string {
  if (typeof v === 'string') {
    const parsed = new Date(v);
    if (!isNaN(parsed.getTime())) {
      return parsed.toLocaleString('fr-FR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    }
    return v;
  }
  return formatUserDate(v);
}

export function mapUserDetailsToDisplay(
  d: UserDetailsDTO,
  getProfilePhotoUrl: (path: string | null | undefined) => string | null
): UserDisplay {
  const name = `${d.firstName || ''} ${d.lastName || ''}`.trim() || d.email;
  const initials = name.split(/\s+/).map(s => s[0]).join('').toUpperCase().slice(0, 2) || '?';
  return {
    id: d.id,
    ref: d.refUser,
    name,
    initials,
    email: d.email,
    role: d.roleLabel,
    createdAt: formatDetailCreatedAt(d.createdAt),
    status: d.isActive ? 'Actif' : 'Inactif',
    phone: d.phoneNumber ?? undefined,
    profilePhotoUrl: getProfilePhotoUrl(d.profilePhotoUrl) ?? undefined,
  };
}
