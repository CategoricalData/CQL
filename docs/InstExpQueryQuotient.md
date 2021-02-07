Expects a list of queries, on per entity, e.g.,
<pre>
entity E -> from x:E y:E where x.a = y.b
entity F -> from x:F y:F
</pre>
each query should have no attributes and no foreign keys.  The resulting matched pairs  (x,y)  are treated
as equations for performing the quotient.  
By default uses a chase-based algorithm; to disable, set quotient_use_chase = false