import { OrderPage } from '../../userRoutes'
export default async function Page({ params }) { const { orderReference } = await params; return <OrderPage reference={orderReference} /> }
