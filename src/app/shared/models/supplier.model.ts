export enum SupplierType {
  GROSSISTE = 'GROSSISTE',
  PRODUCTEUR = 'PRODUCTEUR',
  IMPORTATEUR = 'IMPORTATEUR'
}

export const SupplierTypeLabels: Record<SupplierType, string> = {
  [SupplierType.GROSSISTE]: 'Grossiste',
  [SupplierType.PRODUCTEUR]: 'Producteur',
  [SupplierType.IMPORTATEUR]: 'Importateur'
};

export interface SupplierListItemDTO {
  id: number;
  name: string;
  categoryNames?: string;
  type?: SupplierType;
  contactName?: string;
  phone: string;
  email: string;
  isActive: boolean;
}

export interface SupplierListResponseDTO {
  content: SupplierListItemDTO[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface SupplierDetailsDTO {
  id: number;
  name: string;
  type?: SupplierType;
  categories?: { id: number, name: string }[];
  description?: string;
  address: string;
  phone: string;
  email: string;
  contactName?: string;
  ninea?: string;
  deliveryTime?: string;
  isActive: boolean;
}

export interface CreateSupplierDTO {
  name: string;
  type?: SupplierType;
  categoryIds?: number[];
  description?: string;
  address: string;
  phone: string;
  email: string;
  contactName?: string;
  ninea?: string;
  deliveryTime?: string;
  isActive?: boolean;
}

export interface UpdateSupplierDTO {
  name?: string;
  type?: SupplierType;
  categoryIds?: number[];
  description?: string;
  address?: string;
  phone?: string;
  email?: string;
  contactName?: string;
  ninea?: string;
  deliveryTime?: string;
}

export interface UpdateSupplierStatusDTO {
  isActive: boolean;
}

export interface SupplierStatsDTO {
  totalSuppliers: number;
  activeSuppliers: number;
  inactiveSuppliers: number;
}
