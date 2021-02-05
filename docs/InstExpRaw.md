A literal instance, given by generating rows and labelled nulls, and ground equations.  As always, quotes can be used; for example, to write negative numbers.  Convient additional syntax:<pre>
multi_equations
	name -> person1 bill, person2 alice
</pre>
is equivalent to
<pre>
equations
	person1.name = bill
	person2.name = alice
</pre>
The key-value pairs in multi-equations must be comma separated (necessary for readability and error correction).

See require_consistency, and interpret_as_algebra, which interprets the instance as a model, similar to JDBC / CSV import.  This behavior can be useful when writing down an instance that is already saturated and for which one wants to check the schema constraints, rather than impose them. See All_Syntax for an example.