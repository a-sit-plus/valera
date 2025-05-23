package at.asitplus.wallet.app.common.data.primitives

interface Bijection<DomainElement, CodomainElement> {
    fun forwards(domainElement: DomainElement): CodomainElement

    fun backwards(codomainElement: CodomainElement): DomainElement

    operator fun <ThirdDomain> plus(other: Bijection<CodomainElement, ThirdDomain>): Bijection<DomainElement, ThirdDomain> = object :
        Bijection<DomainElement, ThirdDomain> {
        override fun forwards(domainElement: DomainElement) = other.forwards(this@Bijection.forwards(domainElement))

        override fun backwards(codomainElement: ThirdDomain) = this@Bijection.backwards(other.backwards(codomainElement))
    }

    companion object {
        fun <Domain> identity() = object : Bijection<Domain, Domain> {
            override fun forwards(domainElement: Domain) = domainElement
            override fun backwards(codomainElement: Domain) = codomainElement
        }
    }
}