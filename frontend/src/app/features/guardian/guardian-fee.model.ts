/** Handed back after creating a Razorpay order -- razorpayKeyId is the public key only, never a
 *  secret. Passed straight into Razorpay's hosted Checkout.js widget. */
export interface RazorpayOrderResponse {
  razorpayOrderId: string;
  amount: number;
  currency: string;
  razorpayKeyId: string;
}
