package catdata.cql.gui;

public class SynchExampleHidden extends SynchExample {

	@Override
	public String getName() {
		return "Finance 2";
	}

	@Override
	public String getLinks() {
		return """
				entity_isomorphisms
					client_Client_to_HoldPos : Client.client -> HoldPos.client
					asset_Ref_to_Trans : Ref.asset     -> Trans.asset
					currency_Ref_to_Trans: Ref.currency  -> Trans.currency
					strategy_Ref_to_Portfolio : Ref.strategy  -> Portfolio.strategy
				equations
					e1: forall x:Client.client, x.id          = x.client_Client_to_HoldPos.no

					e2: forall x:Ref.strategy, x.id          = x.strategy_Ref_to_Portfolio.id

					e3: forall x:Ref.asset, x.id          = x.asset_Ref_to_Trans.id

					e4: forall x:Ref.currency, x.id   = x.currency_Ref_to_Trans.id
				constraints

				""";
	}

}