/*
Problem 3: Given this source table 

create table DeptEmp(deptId int not null, mgrName varchar(32), empId int not null)

Populate these target tables

create table Dept(deptId int not null, mgrId int not null, mgrName varchar(32))

create table Emp(empId int not null, wrksIn int not null)

Such that

1) If (d,m,n) is in Dept, then (m,d) is in Emp
2) If (e,d) is in Emp, then (d,M,N) is in Dept for some M and N 
3) If (d,n,e) is in DeptEmp, then (e,d) is in Emp and (d,M,n) is in Dept for some M

Example input data:

insert into DeptEmp(0, Alice, 1);
*/

typeside Ty = literal {
	java_types
		dom = "java.lang.Object"
	java_constants
		dom = "return input[0]"
}

//////////////////////////////////////////////////////////////////////////

schema S = literal : Ty {
	entities
		SDeptEmp
	attributes
		SdeptId : SDeptEmp -> dom
		SmgrName : SDeptEmp -> dom
		SempId : SDeptEmp -> dom
} 

schema T = literal : Ty {
	entities
		TDept TEmp
	attributes
		TempId : TEmp -> dom
		TwrksIn : TEmp -> dom
		TdeptId : TDept -> dom
		TmgrId : TDept -> dom
		TmgrName : TDept -> dom
} 

schema ST = literal : Ty {
	imports 
		S T
} 

//all EDs are expressed on the combined schema ST.  
constraints theEDs = literal : ST {
	//managers work in the departments they manage (stronger than FK)
	//Dept(d,m,n) -> Emp(m,d)
	forall d:TDept ->
 	exists e:TEmp 
 	where d.TdeptId = e.TwrksIn  d.TmgrId = e.TempId
 
	//every employee works in some department (FK)
	//Emp(e,d) -> exists M,N. Dept(d,M,N)
 	forall e:TEmp ->
	exists d:TDept
 	where d.TdeptId = e.TwrksIn	
 	
	//every DeptEmp is a department and an employee
	//DeptEmp(d,n,e) -> exists M. Dept(d, M, n) , Emp(e, d)
	forall de:SDeptEmp ->
	exists d:TDept e:TEmp
 	where de.SdeptId = d.TdeptId d.TdeptId = e.TwrksIn de.SdeptId = d.TdeptId de.SempId = e.TempId
} 

/////////////////////////////////////////////////////////////////////////

//test instance (on ST) DeptEmp(cs, alice, 1)
instance I = literal : ST {
	generators
		de:SDeptEmp
	equations
		de.SdeptId = cs de.SmgrName = alice de.SempId = "1"
}

instance J = chase theEDs I 

command JisOk1 = check theEDs J //verify the chased instance satisfies the EDs

//command InotOk = check theEDs I //the input instance does not satisfy the EDs

