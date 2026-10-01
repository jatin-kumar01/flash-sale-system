import { CheckoutPage } from '../../userRoutes'
export default async function Page({ params }) { const { id } = await params; return <CheckoutPage id={id} /> }
