export interface ProductImage {
  id: number;
  productId: number;
  originalFileName: string | null;
  originalContentType: string | null;
  isPrimary: boolean;
  createdAt: string;
}
