import { ProductPage } from '../../userRoutes'
export default async function Page({ params }) { const { id } = await params; return <ProductPage id={id} /> }
