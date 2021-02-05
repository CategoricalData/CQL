/*
Problem 2: starting from these schemas:

create table Amphibian(id varchar(32) primary key not null,
	name varchar(32),
	land varchar(32) not null,
	water varchar(32) not null);

create table LandAnimal(id varchar(32) primary key not null,
	name varchar(32));

create table WaterAnimal(id varchar(32) primary key not null,
	name varchar(32));

alter table Amphibian add constraint a1 foreign key (land) references LandAnimal (id);
alter table Amphibian add constraint a2 foreign key (water) references WaterAnimal (id);

Sample data:

insert into Amphibian  values ('a1','gecko','l1','w2'), ('a2','frog','l2', 'w3');
insert into LandAnimal values ('l1','gecko'), ('l2','frog'), ('l3','human'), ('l4','cow'), ('l5','horse');
insert into WaterAnimal values ('w1','fish'), ('w2','gecko'), ('w3','frog'), ('w4','dolphin');

Populate the below schemas (the above, plus Animal table, plus 2 new columns in Animal and Amphibian) according to the rule:

 - an amphibian is an animal that is both a land animal and a water animal

create table Amphibian(id varchar(32) primary key not null,
	name varchar(32),
	land varchar(32) not null,
	water varchar(32) not null);

create table LandAnimal(id varchar(32) primary key not null,
	name varchar(32),
	animal varchar(32) not null);

create table WaterAnimal(id varchar(32) primary key not null,
	name varchar(32),
	animal varchar(32) not null);

create table Animal(id varchar(32) primary key not null);

alter table Amphibian add constraint a1 foreign key (land) references LandAnimal (id);
alter table Amphibian add constraint a2 foreign key (water) references WaterAnimal (id);
alter table LandAnimal add constraint l1 foreign key (animal) references Animal (id);
alter table WaterAnimal add constraint w1 foreign key (animal) references Animal (id);
*/


typeside Type = literal {
	external_types
		String -> "java.lang.String"
	external_parsers
		String -> "x => x"
}

schema C = literal : Type {
	entities
		Amphibian
		LandAnimal
		WaterAnimal
	foreign_keys
		IsAL: Amphibian -> LandAnimal
		IsAW: Amphibian -> WaterAnimal
	attributes
		attA: Amphibian -> String
		attL: LandAnimal -> String
		attW: WaterAnimal -> String
}

schema D = literal : Type {
	entities
		yAmphibian
		yLandAnimal
		yWaterAnimal
		yAnimal
	foreign_keys
		yIsAL:yAmphibian->yLandAnimal
		yIsAW:yAmphibian->yWaterAnimal
		yIsALL:yLandAnimal->yAnimal
		yIsAWW:yWaterAnimal->yAnimal
	path_equations
		yAmphibian.yIsAL.yIsALL = yAmphibian.yIsAW.yIsAWW
	attributes
		yattA:yAmphibian->String
		yattL:yLandAnimal->String
		yattW:yWaterAnimal->String
}

mapping F = literal : C -> D {
	entity
		Amphibian->yAmphibian
	foreign_keys
		IsAL -> yIsAL
		IsAW -> yIsAW
	attributes
		attA -> yattA

	entity
		LandAnimal->yLandAnimal
	attributes
		attL -> yattL

	entity
		WaterAnimal->yWaterAnimal
	attributes
		attW -> yattW
}

instance I = literal : C {
	generators
		a1 a2 : Amphibian
		l1 l2 l3 l4 l5 : LandAnimal
		w1 w2 w3 w4 : WaterAnimal
	equations
		 attA(a1) = gecko attA(a2) = frog
		 attL(l1) = gecko attL(l2) = frog
		 attL(l3) = human attL(l4) = cow
		 attL(l5) = horse attW(w1) = fish
		 attW(w2) = gecko attW(w3) = frog
		 attW(w4) = dolphin IsAL(a1) = l1
		 IsAL(a2) = l2 IsAW(a1) = w2 IsAW(a2) = w3
}

instance J = sigma F I
