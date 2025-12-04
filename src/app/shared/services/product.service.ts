import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface Product {
    id: string;
    name: string;
    reference: string;
    category: string;
    price: string;
    stock: number;
    updatedAt: string;
    status: 'Actif' | 'Inactif';
    icon: string;
    description?: string;
}

@Injectable({
    providedIn: 'root'
})
export class ProductService {
    private productsSubject = new BehaviorSubject<Product[]>([
        {
            id: '1',
            name: 'Riz parfumé 25kg',
            reference: 'CP-2025-05',
            category: 'Épicerie',
            price: '16 500 F',
            stock: 42,
            updatedAt: '05/10/2025',
            status: 'Actif',
            icon: '/icones/riz.svg',
            description: 'Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry\'s standard dummy text ever since the 1500s,'
        },
        {
            id: '2',
            name: 'Huile 5L',
            reference: 'CP-2025-04',
            category: 'Épicerie',
            price: '8 500 F',
            stock: 30,
            updatedAt: '05/10/2025',
            status: 'Actif',
            icon: '/icones/huile.svg'
        },
        {
            id: '3',
            name: 'Eau 1.5L (x6)',
            reference: 'CP-2025-03',
            category: 'Boissons',
            price: '2 100 F',
            stock: 80,
            updatedAt: '05/10/2025',
            status: 'Actif',
            icon: '/icones/eau.svg'
        },
        {
            id: '4',
            name: 'Lait 1L',
            reference: 'CP-2025-02',
            category: 'Frais',
            price: '900 F',
            stock: 55,
            updatedAt: '05/10/2025',
            status: 'Actif',
            icon: '/icones/lait.svg'
        },
        {
            id: '5',
            name: 'Savon 250g',
            reference: 'CP-2025-01',
            category: 'Hygiène',
            price: '350 F',
            stock: 120,
            updatedAt: '05/10/2025',
            status: 'Inactif',
            icon: '/icones/savon.svg'
        }
    ]);

    products$ = this.productsSubject.asObservable();

    getProducts(): Product[] {
        return this.productsSubject.value;
    }

    addProduct(productData: { name: string; category: string; price: number; stock: number; description: string; icon: string }) {
        const products = this.productsSubject.value;
        const newId = String(products.length + 1);
        const newReference = `CP-2025-${String(products.length + 1).padStart(2, '0')}`;
        const today = new Date();
        const updatedAt = `${String(today.getDate()).padStart(2, '0')}/${String(today.getMonth() + 1).padStart(2, '0')}/${today.getFullYear()}`;

        const newProduct: Product = {
            id: newId,
            name: productData.name,
            reference: newReference,
            category: productData.category,
            price: `${productData.price.toLocaleString('fr-FR')} F`,
            stock: productData.stock,
            updatedAt: updatedAt,
            status: 'Actif',
            icon: productData.icon,
            description: productData.description
        };

        this.productsSubject.next([newProduct, ...products]);
    }

    updateProduct(productId: string, updates: Partial<Product>) {
        const products = this.productsSubject.value;
        const updatedProducts = products.map(product =>
            product.id === productId ? { ...product, ...updates } : product
        );
        this.productsSubject.next(updatedProducts);
    }

    deleteProduct(productId: string) {
        const products = this.productsSubject.value;
        const filteredProducts = products.filter(product => product.id !== productId);
        this.productsSubject.next(filteredProducts);
    }
}
