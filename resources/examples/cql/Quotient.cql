typeside Ty = empty

schema Likes = literal : Ty {
	entities
		Like 
		Person
	foreign_keys
		likee : Like -> Person
		liker : Like -> Person
}

schema Connections = literal : Ty {
	entities
		Connection 
}

instance SimpsonsLikes = literal : Likes {
	generators
		Ned Maud Rodd Todd MrBurns Smithers : Person
		l1 l2 l3 l4 : Like
	equations
		l1.liker = Ned  l1.likee = Maud
		l2.liker = Maud l2.likee = Rodd
		l3.liker = Rodd l3.likee =  Todd
		
		l4.liker = Smithers l4.likee = MrBurns
}

mapping FindConnections = literal : Likes -> Connections {
	entity
		Person -> Connection

	entity
		Like -> Connection
	foreign_keys
		likee -> identity
		liker -> identity	
}

instance SimpsonsConnections = sigma FindConnections SimpsonsLikes

transform whichConnection = unit FindConnections SimpsonsLikes
