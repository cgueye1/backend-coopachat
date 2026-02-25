// DTO pour l'objet utilisateur
export interface UserDto {
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  role: 'Commercial' | 'Responsable Logistique';
  companyCommercial?: string;
}

