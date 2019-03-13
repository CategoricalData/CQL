options
	prepend_entity_on_ids = false
	jdbc_quote_char = "`"
	talg_reduction=1 
	simplify_names=false

html { (* "
<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"> 
 <html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">
 <head>
 <link rel=\"stylesheet\" type=\"text/css\" href=\"https://categoricaldata.net/css/simple.css\" />
 <script src=\"https://categoricaldata.net/js/simple.js\"></script>
 </head>
 <body>
" *) } 

md { (* "

# CQL Tutorial
**Tasks <-> Skills <-> People**

---

" *) }

md { (* "
## PREFACE - CQL and SQL

This CQL file interacts with a MySQL database running on <a href=\"http://catinf.com\">catinf.com</a>.  To run the tutorial, it
is necessary to <a href=\"https://dev.mysql.com/downloads/connector/j/5.1.html\">download the
MySQL JDBC driver</a> and place it on the java classpath as described in the CQL manual. 
	" *) }


md { (* "
## INTRODUCTION

This file contains a sequence of models intended to illustrate the use of Algebraic Query
Language (CQL) to formalize database concepts in a mathematically verifiable way. Each CQL file 
in this sequence will build on previous ideas, introducing CQL features in the context of
conceptual models relating skills to tasks and people.

The following is a brief outline of the sequence of models and what the reader should take away	
from reviewing each model in the sequence.

This file introduces a series of CQL constructions and compares those constructions with
analogous relational constructions where appropriate. We begin with a set of independent schemas
and produce a single schema that is an integrated assembly of the three source schemas. The
following is a table of contents describing the structure of the sequence of CQL constructions
and concepts developed in this file. The reader should not expect to understand this list at this
point. Several passes through the tutorial may be required to fully grasp all that is presented. 
This file is an executable which when run will generate an execution graphical user interface 
where the results can be viewed. Similarly this file will be published to a series of web pages
where the results of each construction will be displayed.

  1. typeside -- User defined kind for customization of data types.
  2. schema -- Extends the type side to define objects of kind schema.
  3. category -- Polymorphic abstraction that unifies many CQL concepts and constructions.
  4. schema as category -- Schemas interpreted through the abstract lens of categories.
  5. instance -- Extends the schema kind. Instance kinds can be thought of as the data that 
     \"fills\" the schema.
  6. instance as category -- Instances interpreted through the abstract lens of categories.
  7. transform -- map from one instance to another of the same schema.
  8. the category of instances of a schema -- Instance kinds all typed by the same schema
     related by transforms as arrows.
  9. instance coproduct -- Universal construction that \"adds\" multiple instances associated 
     with a schema. 
 10. instance coequalizer -- Universal construction that uses a linking instance and a pair
     of transforms to merge records records within an instance.
 11. schema mappings, queries and the category of schemas -- Schema mappings define the
     arrows in the category of schemas. Queries generalize schema mappings and provide 
     functionality similar to SQL queries.
 12. schema colimits -- A kind that unifies coproducts and coequalizers but in the context of
     the category of schemas rather than the category of instances of a schema. Generally
     formalizes a diagram of schemas connected by mappings that can be used to assemble
     source schemas into a single target schema.
 13. jdbc pragmas, imports and exports -- Integration of CQL with external data sources and 
     targets. Pragmas are a kind that allows commands to be submitted to a JDBC driver to be 
     executed against an external repository. Imports and exports are features that provide a 
     mechanism for translating between traditional data repositories and CQL semantics.
 14. Migrating data between schemas -- This is the problem of Extracting - Transforming -
     Loading or ETL. This is where we tackle problems that involve moving data between 
     schemas and the larger problem of bringing multiple source schemas together and 
     integrating them.
 15. sigma --| delta --| pi -- The machinery CQL uses to move data around the enterprise. 
     These mappings are the building blocks of the eval construction that moves data through 
     CQL queries.

As we proceed we will draw analogies to relational constructions that may be more familiar to
practicing database engineers. We open with a brief review of the essential features of
relational algebra as they are related to CQL features.

The structural element that forms the foundation of relational algebra is the relvar. A relvar is
a mathematical definition that roughly corresponds to the structure of a single table. Commercial
implementations of relvars are table definitions based on a set of fixed types provided by the 
relational database management system (RDBMS) that define the domain over which each column of 
the table is allowed to vary. Having defined one or more table structures, constraints can be
added to further restrict the values over which a particular column may vary. 

In contrast, CQL provides a declarative structure that allows the user to define an algebraic
definition of the type side. In its current implementation CQL defines this algebra by wrapping
a selection of Java types and functions. In this sequence of files we will restrict the type side
to a string type compatible with a MySQL Varchar. This will allow us to label records in a way
that will provide human readable output associated with the sequence of models we produce. This 
will also allow us to integrate with an external RDBMS by exporting and importing various CQL 
constructions.

CQL models always begin with a type side declaration. The type side defines a foundation that can 
then be extended to define schemas.
" *) }

md { (* "
---

## Type Sides

Type sides are an CQL construction that extends the built in types typically found in RDBMSs and
provides the possibility of introducing programming language features that can be used to enhance 
the power of a database. These features are roughly analogous to stored functions that can be 
invoked inline within an SQL statement. To illustrate how this capability works we will create a 
type side that exposes a string type which we label Varchar in anticipation of later integration 
with an external RDBMS. To this we add the Java string matches method to illustrate how Java 
functions can be exposed to the CQL framework. For more details we refer the reader to the 
examples available by selecting the dropdown in the upper right hand corner of the application. 
Specifically, the CQL Tutorial example.
" *) }

typeside TypeSide = literal {
    java_types
        Varchar = "java.lang.String"
        Bool = "java.lang.Boolean"
    java_constants
        Varchar = "return input[0]"
        Bool = "return java.lang.Boolean.parseBoolean(input[0])"
    java_functions
        Matches : Varchar, Varchar -> Bool = "return input[0].matches(input[1])"
}


md { (* "
---

## SCHEMAS

As mentioned above the schema is declared as an extension of the type side. Notice that in
contrast with relational algebra there is no notion of an entity or relvar in isolation. The
closest analog to a relvar in CQL is a schema with a single entity. With that in mind we proceed
by creating a pair of schemas, each with a single entity, followed by the schema with a pair of
entities connected by a foreign key.

The first schema we create is minimal in that it contains a single entity Task with a single
attribute that allows us to label a task. CQL contains a variety of constructions that build on 
on previously defined constructions. Following the programming language literature we will refer 
to such constructions as kinds. In CQL a kind is either a typeside, schema, instance, (schema) 
mapping, (instance) transform, query, graph, pragma, schema\_colimit, or constraints.

By convention we prefix the schema name with the letter \"s\" to remind the reader when that name 
is encountered in the construction of a dependent kind that the object is of kind schema. 

By clicking on the run button CQL will construct a runtime viewing window that has a list of each
of the constructions created in this model in a pane on the left hand side of the window. A view
of each construction is available by clicking on the construction of interest in the left hand
pane.

Similarly this editor has a pane on the right hand side containing an outline of the
constructions contained in this file. One can navigate to a construction by clicking on that
construction in the outline.
" *) }

schema sT = literal : TypeSide {
    entities
        Task
    attributes
        TName : Task -> Varchar
}

md { (* "
Now we create a slightly more interesting schema. This schema still consists of only one entity,
but that entity now has a pair of attributes.
" *) }

schema sSnT = literal : TypeSide {
    entities
      SkillNeededForTask
    attributes
      skill : SkillNeededForTask -> Varchar
      task : SkillNeededForTask -> Varchar
}


md { (* "
---

## CATEGORIES INTRODUCED
**Schemas as Categories**

The third schema we introduce has two entities, `Skill` and `SkillClass` that define a simple two
level taxonomy for skills. This brings into play additional sections as part of the declarative
structure of a schema. First the `foreign_keys` section. In CQL, schemas as first class objects come
equipped with all of the machinery for defining a schema as a kind. This includes declaration of
dependencies between entities. These dependencies are given the name foreign key so as to be
consistent with relational nomenclature.

Finally we have a third section that has no analog in SQL systems, the observation_equations section.
To understand this section we must take time for a brief digression to introduce the abstract 
notion of a category. 

CQL is motivated by a branch of mathematics known as category theory. The reader who is interested 
in a systematic development of category theory should refer to any one of a number of excellent
introductions. Here are several such references:

  * _Category Theory_ by Steve Awodey
  * _Category Theory for Computing Science_ by Michael Barr and Charles Wells
  * _Category Theory for the Sciences_ By David Spivak
  * _Conceptual Mathematics: A first introduction to categories_ by F. William Lawvere and Stephen
    H. Schanuel.

We begin with an intuitive description of categories and identify specific concrete examples as 
they relate to CQL. This is followed by a formal description of a category.

A category consists of a set of objects connected by arrows. As such, a category is related to a
directed graph where the objects are nodes and arrows are edges with direction indicated by their
associated arrow. In addition to the properties of a directed graph each object of a category is
required to have a distinguished arrow that identifies that object and whose source and target are
equal to the object it identifies.

Let's consider the three schemas we have so far constructed, but with an empty type side (i.e. no
attributes). Each such schema could be viewed as an abstract category where the set of objects is
the set of entities and its arrows are foreign keys. Hence schemas are the first example of an CQL 
construction that can be interpreted as a category.

With this in mind `sT` and `sSnT` are both one object categories. By virtue of the fact that the set 
of identity arrows and the set of objects are essentially the same set (technically isomorphic
to each other) we can unambiguously refer to each identity arrow using the same label as its
corresponding entity. Similarly `sS` (again neglecting attributes) is a two object category with
one non-identity arrow, the foreign key `fk_HasClass`. This category is sometimes called the \"arrow
category\". In future examples we will see that more generally schemas with empty type sides are
not strictly speaking categories, but rather presentations of categories. This means they have
all the information necessary to generate a category through a process known as saturation.

Abstract categories are by design devoid of attachments to any specific context. In order to
leverage the context free nature of category theory we need a mechanism by which we can anchor
a category into a computational environment. Functional programming languages such as Haskell
provide one strategy for doing this. CQL provides another strategy that is motivated by the
capabilities of modern database management systems, so CQL extends the basic notion of an
abstract category with a type side. We will rely on the above intuitive motivation and will not
formally develop this idea here. The interested reader is referred to the research paper upon 
which the CQL implementation is based:

[https://arxiv.org/abs/1602.03501](https://arxiv.org/abs/1602.03501)

There is an additional feature of categories that is critical to this discussion and that lies
at the heart of the expressive power of category theory. This the notion of a path. A category
by definition contains all paths that can be generated by composing arrows head to tail. This
process of generating all paths implied by arrow composition is the aforementioned saturation.

The order in which one chooses to compose arrows has no effect on the result so CQL provides a
convenient syntax for indicating a path using the dot notation (elaborated below). Having added
additional arrows for each path we now have the ability to declare two paths that begin and end 
on the same object to be equal and so the declarative structure of a category includes equations.

Path equations can be used to complete an intuitive explanation of the properties of identity 
arrows. Any path that includes an identity arrow is equal to the path obtained when that identity 
arrow is omitted. Considering arrow composition as analogous to addition we can think of identity
arrows as analogous to zero, but rather than having one zero we have one for each object in the
category. Category theorists make this analogy by saying that identity arrows are paths of length
zero. To further strengthen this analogy it can be shown that the counting numbers under the
binary operation of addition define a one object category. Each arrow of the category is a
counting number. Zero is the identity arrow. Addition is arrow composition.


**Category Definition**

Here is a formal definition of a category following the presentation in Category Theory for 
Computing Sciences chapter 2.

We begin with a formal definition of a directed graph:
A directed graph `C` is a pair of functions source: `C_1` -> `C_0` and target: `C_1` -> `C_0`. The elements
of `C_0` we call nodes/objects and the elements of `C_1` we call edges/arrows. An arrow path of
length `n` in `C` is a sequence of arrows arranged head to tail as follows:

         f_n    f_n-1    ...     f_2     f_1
      . ----> . ----> .  ...  . ----> . ----> . 

" *) }

/* Rendered using Javascript:
html { (* "
$$
\\begin{CD}
 \\cdot     @>f_n>> \\cdot  @>f_{n-1}>> \\cdot @. \\ldots @. \\cdot @>f_2>> \\cdot @>f_1>> \\cdot
\\end{CD}
$$
" *)
}
*/

md { (* "
For any adjacent pair of arrows in such a path we must have `target(f_k)` = `source(f_k-1)`. Such
adjacent pairs we call composable pairs. In such a graph one can define sets of paths of length `n`
which we designate `C_n`.

The definition of a category extends the definition of a directed graph:
A category is a graph `C` together with two functions `c: C_2` -> `C_1` and `u: C_0 -> C_1` with
properties `C-1` through `C-4` below. The function `c` we call composition, and if `(f, g)` is a
composable pair, `c(f,g)` is written `f.g` and is called the composite of `f` followed by `g`. 
If `A` is an object of `C`, `u(A)` is denoted `id_A` or just `A`, which is called the identity of
the object `A`.

  * C-1 The source of `f.g` is the source of `f` and the target of `f.g` is the target of `g`.
  * C-2 `(f.g).h = f.(g.h)` whenever either is defined.
  * C-3 The source and target of `id_A` are both `A`.
  * C-4 If `f: A -> B`, `id_A.f = f.id_B = f` (alternatively `A.f = f.B = f`).

We now introduce the fourth section in the schema declaration below, the `observation_equations` 
section. The `observation_equations` section is an example of a path equation. Notice that 
`SCNameOnSkill` is an arrow from `Skill` to `Varchar`. As such it is a path of length one. Similarly
there is a path from `Skill` to `Varchar` that is a path of length two obtained by composing the 
foreign key arrow `fk_HasClass` with the attribute arrow `SCName`. The observation equation is a 
declaration that those two paths are equal.

The practical significance of this is far reaching, but in this case it provides us with a
mechanism to denormalize our schema while guaranteeing that no inconsistencies will be allowed. 

Much of the power of category theory comes from its polymorphic nature. Many different contexts
can be interpreted through the abstract lens of category theory. We will see in the coming
discussion that many of the kinds supported by CQL can be interpreted using category theory.
" *) }

schema sS = literal : TypeSide {
    entities
        Skill SkillClass
    foreign_keys
        fk_HasClass : Skill -> SkillClass
    attributes
        SName : Skill -> Varchar
        SCNameOnSkl : Skill -> Varchar
        SCName : SkillClass -> Varchar
    observation_equations
        SCNameOnSkl = fk_HasClass.SCName
}


md { (* "
---

## INSTANCES

As schemas extend the type side so instances extend schemas. An CQL instance can be
conceptualized as the data that \"fills\" a schema. Minimally, an instance consists of a set of 
generators for each entity in the schema. A generator is a string used by CQL that serves to
identify a particular record in an entity. The user can optionally create equational definitions 
which associate the generator of one entity with that of another or a generator with an attribute 
value. In the absence of such an association CQL will create what is called a Skolem term. Skolem 
terms are essential for creating a database instance that can be formally defined and verified as 
consistent. Skolem terms at a type are referred to as a labeled null. Skolem terms at an entity 
are referred to as a meaningless identifier. Notice that labeled nulls are very different than 
nulls in currently available RDBMSs.

Every null in CQL is typed by its associated generator and the path to the location in a schema 
where a value is missing. For example, in the instance definition below we failed to provide any 
generators for skill classes and so CQL created a Skolem term for each unknown skill class 
beginning with a skill generator followed by the foreign key path. So for the Math skill there 
is an unknown generator `s0.fk_HasClass` whose unknown name is `s0.fk_HasClass.SCName`.

The following is a representative instance of skills. The way to think about this representative
is that it is one member of a universe of possible instances all of which are derived from the
same schema type `sS`. To summarize, so far we have introduced the idea that CQL constructions are 
organized around the kind hierarchy:

typeside <- schema <- instance.

Running this file and selecting an instance below will display a tabular representation of each
instance.
" *) }
instance iSkl = literal : sS {
    generators
        s0 s1 s2 s3 s4 s5 s6 s7 s8 s9 : Skill
    multi_equations
        SName -> {s0 CategoryTheory, s1 CompSci, s2 DBA, s3 Pharma, s4 Programming, s5 Lecturing, 
                  s6 GrantWriting, s7 Finance, s8 Management, s9 PaperWriting}
}

md { (* "
Analogous to the skills instance above we create a representative instance for tasks.
" *) }

instance iTsk = literal : sT {
    generators
        t1 t2 t3 t4 t5 t6 t7 t8 t9 t10 : Task
    multi_equations
    TName -> {t1 LaunchCIStartup, t2 BuildCQL, t3 WriteCQLTutorial, t4 BuildPhrmColim, t5 BuildFncColim,
              t6 WriteCTPaper, t7 TeachCT, t8 TeachCS, t9 RunPhrmIntProject, t10 RunFncIntProject}
}

md { (* "
The definition of an entity with a pair of attributes provides us with the ability to create an
instance that associates each such pair with the same generator. Here we illustrate this idea
with such a representative instance.
" *) }
instance iSnT = literal : sSnT {
    generators
        st1 st2 st3 st4 st5 st6 st7 st8 st9 st10
        st11 st12 st13 st14 st15 st16 st17 st18 st19 st20
        st21 st22 st23 st24: SkillNeededForTask
    multi_equations
        skill -> {st1 CompSci,            st2 Programming,      st3 GrantWriting,    st4 PaperWriting,
                  st5 CompSci,            st6 Programming,      st7 Management,
                  st8 Programming,        st9 Lecturing,        st10 PaperWriting,
                  st11 Pharma,            st12 Programming,
                  st13 Finance,           st14 Programming,
                  st15 CategoryTheory,    st16 PaperWriting,
                  st17 CategoryTheory,    st18 Lecturing,
                  st19 Pharma,            st20 Management,
                  st21 Finance,           st22 Management,
                  st23 CompSci,           st24 Lecturing}
        task ->  {st1 LaunchCIStartup,    st2 LaunchCIStartup,  st3 LaunchCIStartup, st4 LaunchCIStartup,
                  st5 BuildCQL,           st6 BuildCQL,         st7 BuildCQL,
                  st8 WriteCQLTutorial,   st9 WriteCQLTutorial, st10 WriteCQLTutorial,
                  st11 BuildPhrmColim,    st12 BuildPhrmColim,
                  st13 BuildFncColim,     st14 BuildFncColim,
                  st15 WriteCTPaper,      st16 WriteCTPaper,
                  st17 TeachCT,           st18 TeachCT,
                  st19 RunPhrmIntProject, st20 RunPhrmIntProject,
                  st21 RunFncIntProject,  st22 RunFncIntProject,
                  st23 TeachCS,           st24 TeachCS}
}


md { (* "
**Instance Categories**

Like schemas, instances can also be viewed as categories. To make this clearer consider these
alternative syntaxes for declaring an instance using the equations keyword. The following are
both equivalent to the `iSkl` declaration above:

    instance iSkl = literal : sS {
        generators
            s0 s1 s2 s3 s4 s5 s6 s7 s8 s9 : Skill
        equations
          s0.SName = CategoryTheory    s1.SName = CompSci        s2.SName = DBA
          s3.SName = Pharma            s4.SName = Programming    s5.SName = Lecturing
          s6.SName = GrantWriting      s7.SName = Finance        s8.SName = Management
          s9.SName = PaperWriting
    }
    instance iSkl = literal : sS {
        generators
            s0 s1 s2 s3 s4 s5 s6 s7 s8 s9 : Skill
        equations
          SName(s0) = CategoryTheory    SName(s1) = CompSci        SName(s2) = DBA
          SName(s3) = Pharma            SName(s4) = Programming    SName(s5) = Lecturing
          SName(s6) = GrantWriting      SName(s7) = Finance        SName(s8) = Management
          SName(s9) = PaperWriting
    }

The objects of an instance category are the generators plus the Skolem terms produced by those
generators. The arrows are either a foreign key evaluated at a generator/Skolem term or an 
attribute evaluated at a generator/Skolem term. Like a schema the literal declaration of an 
instance is a presentation of an instance category. As you will see when you run an CQL model 
with a literal instance declaration CQL will fill Skolem terms as implied by the schema and the 
provided generators.

Notice these equations declare instance arrows in a way that is closely analogous to an RDF 
triple. The subject of a triple is a generator/Skolem term, the object of a triple is either a 
literal (value on the type side) or another generator/Skolem term and the predicate is the type 
on the arrow as determined by the underlying schema. As such CQL instances are closely related to 
graph databases although they are very strongly typed by their underlying schema.

In order to extend the instance `iSkl` to include classes we need to decide how we want to
partition the skills we have into broad equivalence classes (we list the generators for 
convenience):

  * Class 1: CategoryTheory, CompSci, DBA, Programming (s0, s1, s2, s4)
  * Class 2: Pharma, Lecturing, Finance, Management (s3, s5, s7, s8)
  * Class 3: GrantWriting, PaperWriting (s6, s9)
  
CQL provides the ability to extend any construction by importing a construction already in place,
so using the path dot notation we extend the `iSkl` instance as follows:
" *) }

instance iSklExt = literal : sS {
    imports
       iSkl
    equations
       s0.fk_HasClass = s1.fk_HasClass
       s0.fk_HasClass = s2.fk_HasClass
       s0.fk_HasClass = s4.fk_HasClass
       s3.fk_HasClass = s5.fk_HasClass
       s3.fk_HasClass = s7.fk_HasClass
       s3.fk_HasClass = s8.fk_HasClass
       s6.fk_HasClass = s9.fk_HasClass
}

md { (* "
Notice that each of the paths in the equations above have a direct correspondence with a Skolem 
term. Hence, in contrast with traditional database NULL values, Skolem terms can be manipulated 
in the same way any other value can be manipulated. If you compare `iSkl` and `iSklExt` in the 
runtime GUI you will note that the number of Skolem terms has decreased from ten (one for each 
skill) to three (one for each equivalence class).
" *) }

md { (* "
**The Category of Instances of a Schema**

Now lets see if we can give these skill class labeled nulls labels. To do this we have to
introduce another example of a category, the universe of possible instances that are all based on 
a particular schema. For example all instances of `sS` define a category. The objects are instances 
of `sS` and we already have two examples, `iSkl` and `iSklExt`. The arrows are transforms from one
instance of `sS` to another instance of `sS`. A transform is an instance map that preserves the 
structure of the instance. We will return to transforms in a moment, but for now lets create a
third instance with labels for our three skill classes.
" *) }

instance iSklCls = literal : sS {
    generators
        sc0 sc1 sc2 : SkillClass
    equations
        sc0.SCName = Cognitive
        sc1.SCName = Social
        sc2.SCName = Psychomotor
}

md { (* "
An important idea in category theory is the notion of a universal construction. We now introduce
the CQL implementation of such a construction. The idea is that given a particular structure
defined by a diagram one can consider all possible examples of that structure in a particular
category. By that we mean all examples that have the same shape as the diagram in question. Let's
consider a diagram called a cospan. It has this shape: `t1: A --> X <-- B :t2`. As we were 
currently talking about the category of instances of the schema `sS`, the objects `A`, `X`, and `B`
are instances and the two arrows are transforms. A cospan in this category is any valid collection
of three instances of `sS` that are related to one another via a pair of transforms.

Given a choice of instances say `iSklExt` and `iSklCls` we can consider all possible ways of 
combining `iSklExt` and `iSklCls` with a pair of transforms to get a third instance `X`. The
universal way of combining `iSklExt` and `iSklCls` to get an `X` is the one to which all others can
be related in a unique way. That is to say, for any `X` and any pair of valid transforms `t1` and
`t2` the universal cospan `iA: iSklExt --> iSklCP <-- iSklCls :iB` is the cospan such that there
exists a unique transform:

    t(t1,t2): iSklCP --> X such that
    iA.t(t1,t2) = t1 and
    iB.t(t1,t2) = t2

for any `X` and any pair of transforms `(t1, t2)`. What this means is that the pair of arrows `t1`
and `t2` can be replaced with the single arrow `t(t1,t2)`.

Conceptually this construction is a generalization of the SQL \"UNION ALL\" statement. It is a 
generalization in that it applies to a pair of instances of a schema rather than a pair of 
relations of a relvar. This construction is known as a coproduct. We say that `iA` and `iB` are 
injections of the instances `iSklExt` and `iSklCls` into the coproduct. You can see the effect of
this construction by running this file and selecting `iSklCP` in the viewer. Notice the result is
an instance `sS` that is the disjoint union of the records of `iSklExt` and `iSklCls` that has
respected the foreign key `fk_HasClass`.
" *) }
instance iSklCP = literal : sS {
	imports iSklExt iSklCls 
}

md { (* "
Now that we have all of the desired records in the same instance we want to identify the
generators that came from `iSklCls` with the Skolem terms that came from the `iSklExt` instance. To 
achieve this result we would like to declare that each of the Skolem terms from `iSklExt` should be 
merged with the appropriate generator coming from `iSklCls`. Recall we have already done this by 
importing an existing instance into a new instance and adding equations. Here is the solution
using that approach.
" *) }

instance iSklSklCls1 = literal : sS {
    imports
      iSklCP
    equations
      s0.fk_HasClass = sc0
      s3.fk_HasClass = sc1
      s6.fk_HasClass = sc2
}

md { (* "
There is an alternative construction that is universal. First we create an instance `iSLnk` with a 
generator for each of the skill classes. We then create two instance transforms, the first 
mapping each link generator to a `iSklExt` Skolem term and the second mapping each link generator 
to our `iSklCls` generators. The new universal construction is known as a coequalizer. An instance 
coequalizer uses a pair of transforms to define collections of records that are identified as 
equal. This approach has the advantage that linking instances and transforms can be imported from 
an external database using SQL queries.

This construction is a generalization of the notion of partitioning a set into equivalence
classes. Later we will see how these constructions can be applied to other categories in CQL.
" *) }

instance iSLnk = literal : sS {
    generators
        scLnk0 scLnk1 scLnk2 : SkillClass
}
transform tS1 = literal : iSLnk -> iSklCP {
    generators
        scLnk0 -> s0.fk_HasClass
        scLnk1 -> s3.fk_HasClass
        scLnk2 -> s6.fk_HasClass
}
transform tS2 = literal : iSLnk -> iSklCP {
    generators
        scLnk0 -> sc0
        scLnk1 -> sc1
        scLnk2 -> sc2
}
instance iSklSklCls2 = coequalize tS1 tS2

md { (* "
Notice we can summarize the sequence of constructions above as follows:
 
    s0.fk_HasClass <- scLnk0 -> sc0
    s3.fk_HasClass <- scLnk1 -> sc1
    s6.fk_HasClass <- scLnk2 -> sc2,

which in turn is structurally equivalent to the equations in `iSklSklCls1`.
" *) }

md { (* "
---

## CQL QUERIES
**And the Category of Schemas**

The next example of a category we need is the category of schemas. Since a schema can be viewed
as a presentation of a category the category of schemas is closely related to the category of
categories. The objects of the category of schemas are of course schemas and the arrows are
referred to in CQL as mappings. There is a syntax for the literal declaration of a mapping that 
is provided by CQL, but we will not elaborate on that syntax as you will see that most
applications of mappings are either implicit in other constructions or generalizations to
mappings as in the case of queries which we introduce in this section.

Conceptually mappings preserve the structure of a schema, that is, they map entities and foreign
keys/attributes in a consistent way. For the interested reader the name given mappings in the
mathematical literature is functor, as a functor can be viewed as a generalized function. Queries
further generalize mappings in the same way that a cospan `A -> B <- C` generalizes a single arrow 
`A -> C`.

CQL queries can be used to extract some part of a schema instance as well as to calculate derived 
results. This construction is the query kind. We illustrate in our first example a simple form of 
an CQL query that is very similar to an SQL query.

  * The from clause: like SQL lists the entities from which the query is constructed. CQL differs
    in that each entity must be provided with a variable that serves as a representative generator
    for that entity. This is syntactically similar to an SQL alias though not optional.
  * The where clause: Again similar to the where clause in SQL in that it provides a means for
    filtering records and joining entities.
  * The attributes clause: The attributes clause is analogous to the select clause in SQL and defines what
    the query output is.
  
We make use of the Java string method \"matches\" we have exposed through the type side. This
method allows us to pass a regular expression that can be used as a filter for any skill name 
ending in the string \"Writing\".
" *) }

query qWrtngSklS = simple : sS {
    from
      s: Skill
    where
      Matches(s.SName, ".*Writing") = true
    attributes
      Skill_Name -> s.SName
      Skill_Class_Name -> s.SCNameOnSkl
}

md { (* "
By virtue of CQL's ability to define multiple instances associated with a schema, to execute a
query it must be evaluated on an instance to define a new instance. When queries are of the
simple type as in this example the schema is implicitly defined as having a single entity with
attributes as listed in the return clause.
" *) }
instance iWrtngSklS = eval qWrtngSklS iSkl

md { (* "
To illustrate the more complete query syntax we must explicitly declare a target schema. Below
we rewrite `qWrtngSklS` to illustrate the more complete syntax. First we define the target schema
with a single entity `tSkill` and the two attributes listed in the return clause above:
" *) }
schema sSkl = literal : TypeSide {
    entities
      tSkill
    attributes
      Skill_Name : tSkill -> Varchar
      Skill_Class_Name : tSkill -> Varchar
}

md { (* "
Next we create a query whose kind is an arrow from the source schema `sS` to the target schema
`sSkl`. This complete syntax requires a query sub-section for each entity in the target schema. 
Additionally the complete syntax requires an assignment for all attributes defined in the target 
schema. The query `qWrtngSklC` is functionally equivalent to `qWrtngSklS`. If you run this model you 
will notice that the instance `iWrtngSklS` is identical to the instance `iWrtngSklC`.
" *) }
query qWrtngSklC = literal : sS -> sSkl {
    entity
      tSkill -> {
          from
            s: Skill
          where
          Matches(s.SName, ".*Writing") = true
          attributes
          Skill_Name -> s.SName
          Skill_Class_Name -> s.SCNameOnSkl
      }
}
instance iWrtngSklC = eval qWrtngSklC iSkl

md { (* "
Queries as structure preserving maps of schemas have the capability of mapping not only entities
but also foreign keys. In this example we will build a query that has as its source and target 
schemas `sS` but is not an identity. This example filters for those skills that are classified as 
social skills.

Given its novel nature, let's take a moment to elaborate on the syntax of the `foreign_keys` 
section. We have duplicated the declaration of the foreign key we are constructing in a comment as 
reference. The `fk_HasClass` foreign key enforces a functional dependency between the generators of
the entity Skill and the generators of of the entity `SkillClass`. In the entities section we have
generator variables sc for the `SkillClass` entity and `s` for the `Skill` entity. These are variables 
that vary over the generators and Skolem terms created by the saturation process. The task at 
hand is to use those variables and the source foreign key to construct a functional dependency in 
the target schema. We do this by taking a `SkillClass` generator and assigning it as the return 
value of the target foreign key `fk_HasClass`. Which generator do we assign? The one we get by 
taking a skill generator and following it to its skill class. Notice that this is opposite to the 
direction of the arrow associated with the `fk_SkillClass` functional dependency.

Note: If you removed the where clause from each entity sub-section you would have the identity 
mapping.
" *) }
query qScl = literal : sS -> sS {
    entity
      SkillClass -> {
          from
            sc: SkillClass
          where
            sc.SCName = Social
          attributes
            SCName -> sc.SCName
      }
   entity   Skill -> {
          from
            s: Skill
          where
            s.fk_HasClass.SCName = Social
          attributes
            SName -> s.SName
            SCNameOnSkl -> s.SCNameOnSkl
          foreign_keys
      // fk_HasClass : Skill -> SkillClass
      fk_HasClass -> {sc -> s.fk_HasClass}
      }
   
}
instance iSclSkl = eval qScl iSklSklCls1


md { (* "
---

## SCHEMA COLIMIT

Next we introduce an CQL feature that allows us to assemble multiple source schemas into a single
target schema. This construction is called a schema colimit by virtue of its mathematical origins
in category theory. Generally colimits are a class of universal constructions that take a diagram
of objects connected by arrows and assemble those objects in a way consistent with the diagram.
We have already seen an example of a colimit in the instance coproduct. Now we apply CQL's
implementation of that same universal construction to the category of schemas. When applied to
schemas the coproduct results in a construction analogous to what is sometimes called \"landing\"
tables from multiple source schemas into a single schema where data transforms can be 
conveniently developed.

In this particular case we \"land\" the three schemas `sT`, `sS`, `sSnT` using the single structure
`S_T_SnT_SumAuto` whose kind is `schema_colimit`. We will develop the connection of a schema 
coproduct with a instance coproduct in a moment, but for now examine the automatically generated 
coproduct in the runtime viewer.
" *) }
schema_colimit S_T_SnT_SumAuto = coproduct sT + sS + sSnT : TypeSide
//schema_colimit S_T_SnT_SumAuto = quotient sT + sS + sSnT: TypeSide {}

md { (* "
The automatically generated schema colimit prefixes each object within the target schema with the
label associated with the source schema from which that object originated. With the modify
command we have the option of providing whatever names we choose independent of the names
associated with the source schemas from which they originated. In this case we designate each
entity with the prefix `l` to indicate that it was landed using the coproduct operation.
" *) }
schema_colimit S_T_SnT_Sum = modify S_T_SnT_SumAuto {
    rename entities
        sS_Skill -> lSkill
        sS_SkillClass -> lSkillClass
        sT_Task -> lTask
        sSnT_SkillNeededForTask -> lSkillNeededForTask
    rename foreign_keys
        lSkill.sS_Skill_fk_HasClass -> lfk_HasClass
    rename attributes
        lSkill.sS_Skill_SName -> lSName
        lSkill.sS_Skill_SCNameOnSkl -> lSCNameOnSkl
        lSkillClass.sS_SkillClass_SCName -> lSCName
        lTask.sT_Task_TName -> lTName
        lSkillNeededForTask.sSnT_SkillNeededForTask_skill -> lskill
        lSkillNeededForTask.sSnT_SkillNeededForTask_task -> ltask
}

md { (* "
**Schema Colimit Diagram**

Recalling our earlier description of the instance coproduct which was a cospan of instances, a 
schema coproduct is a cospan of schemas and as such includes a schema and a pair of structure
preserving maps of schemas.

With the sequence of constructions below we reveal the inner structure of the particular schema
colimit we have created. In this case there are three source objects and three associated arrows
rather than two as was the case with the instance coproduct. We would call this a wide cospan which
extends the concept of a span to include any number of source objects. Specifically, for this
schema colimit we have a target schema and a schema mapping which lands (injects) each source schema
into that landing (target) schema. This means that a schema colimit can be thought of as a diagram
of schemas and schema mappings. In this particular case, using the labels assigned below, we have
the following diagram:

                     mSToS_T_SnT_Sum        mTToS_T_Snt_Sum
                     sS --------> sS_T_Snt_Sum <-------- sT
                                       /\\
                     mSnTToS_T_SnT_Sum |
                                       |
                                     sSnT

" *) }
schema sS_T_SnT_Sum = getSchema S_T_SnT_Sum
mapping mSToS_T_SnT_Sum = getMapping S_T_SnT_Sum sS
mapping mTToS_T_Snt_Sum = getMapping S_T_SnT_Sum sT
mapping mSnTToS_T_SnT_Sum = getMapping S_T_SnT_Sum sSnT

md { (* "
---

## Migrating Data Between Schemas
**Sigma --| Delta --| Pi**

Let's recap where we are. We have a set of three source schemas `sS`, `sT`, and `sSnT`, each with an
instance; `iSklSklCls1`, `iTsk`, and `iSnT` respectively. We have a target schema `sS_T_SnT_Sum`. We
also have mappings that take each source schema to the target schema. What we want is a construction
similar to eval that can be applied to a mapping rather than a query, but the schema colimit is
giving us mappings not queries.

Recall that queries generalize mappings. It so happens there are three constructions that are 
related to eval in the same way mappings are related to queries. In fact eval and it's dual coeval,
which we have not discussed, are generalizations of these constructions. To develop these ideas
would be a lengthy digression so we will only mention them in passing along with some informal
remarks about their properties. 

These three constructions are called sigma, pi and delta. They all can be understood as moving 
instance data through mappings in a way that is analogous to how eval moves instances through 
queries. This family of functors collectively define CQL's implementation of Extration, 
Transformation and Load abbreviated ETL processes. Sigma, pi and delta are all structure 
preserving maps between the category of instances of one schema to the category of instances of 
another schema, so they are functors. This means that sigma, pi and delta map not only instances 
but instance transforms. These three mappings are related as follows. Delta pulls data backward 
through a mapping and generalizes the notion of projection. Both Sigma and Pi push data forward 
through a mapping and generalize sums/unions and products/joins respectively. Given a single 
schema mapping, Delta can be paired with sigma on the left and pi on the right to create data 
migration round trips.

To see this, consider schemas `sA`, `sB`, mapping `m` and instance `iA` of `sA`. Without yet
specifying specifics the CQL syntax would be:
 
    mapping m = literal : sA -> sB {...} 
    instance iB = literal : sB {...}

Delta can move the instance `iB` backward through `m` into `sA` as follows:
 
    instance iDlt_m_iB = delta m iB
 
Similarly, sigma can move this new instance forward through the same `m` into `sB`:
 
    instance iSgmDlt_m_iB = sigma m iDlt_m_iB
 
A natural thing to ask is how `iSgmDlt_m_iB` and `iB` are related. The answer is they are related by
a transform known as the unit of the sigma --| delta adjoint pair. Notice we could also start
with an instance `iA` of `sA` and apply first sigma `m` and then delta `m` to product an instance 
`iDltSgm_m_iA`. `iA` and `iDltSgm_m_iA` are related by the counit of the sigma --| delta adjoint pair. 
In a similar way delta --| pi form an adjoint pair with associated unit and counit transforms. 
We say that delta is right adjoint to sigma and left adjoint to pi.

Of the three, sigma has the property that generators of the source instance are preserved in the 
target instance. This means we can move instances and transforms between schemas in a way that 
permits us to merge instance data. Let's see how we can use sigma to solve the problem at hand
and later we will return to broader questions about the nature of sigma, pi and delta and their
associated units and counits.

Recall we have a target schema that contains all of the entities from our three source schemas
we can use the sigma construction to push the instances in the source schemas forward through the 
mappings generated by the `schema_colimit` construction to create an instance in the `sS_T_SnT_Sum`
assembly. Having done so we would have three separate instances in the target schema 
`sS_T_SnT_Sum`. With all of these instances in the same schema we can make use of our familiar
instance coproduct construction. This would look like the following:

    instance iSklSklCls_In_S_T_SnT_Sum = sigma mSToS_T_SnT_Sum iSklSklCls1
    instance iTsk_In_S_T_SnT_Sum = sigma mSToT_T_SnT_Sum iTsk
    instance iSnT_In_S_T_SnT_Sum = sigma mSnTToS_T_SnT_Sum iSnT
    instance iS_T_SnT_Sum = coproduct
        iSklSklCls_In_S_T_SnT_Sum
        iTsk_In_S_T_SnT_Sum
        iSnT_In_S_T_SnT_Sum
        : sS_T_SnT_Sum
    
CQL provides the `coproduct_sigma` construction which performs the above sequence in one step:
" *) }

instance A = sigma mSToS_T_SnT_Sum iSklSklCls1
instance B = sigma mTToS_T_Snt_Sum iTsk
instance C = sigma mSnTToS_T_SnT_Sum iSnT
instance iS_T_SnT_Sum = literal : sS_T_SnT_Sum {
	imports A B C
}
  
md { (* "
---

## CQL Queries Continued
 
In the current context we have as source schema the result of the colimit construction. If you
run this file and look at the colimit schema `sS_T_SnT_Sum` and associated instance you will
discover the we have assembled a target schema where the entities of the source schemas 
are independent of one another. We now use the query construction to introduce foreign key 
constraints in a way that is analogous to the addition of the foreign key constraints supported 
in RDBMSs.

Recall that a non-simple CQL query requires we define the target schema first. This schema is
identical to the output of the colimit construction but with the addition of a pair of foreign
keys from the `SkillNdFrTask` entity with the (skill, task) attribute pair to the Skill and Task 
entities respectively. This produces a schema which is known in category theory as a span and it 
can again be summarized as a diagram, but in contrast with the case of the colimit construction 
this is a diagram of entities rather than schemas. We have enclosed the `Skill` and `SkillClass` 
entities with parentheses to indicate that they serve as a reference taxonomy.

              fk_HasClass    fk_SnT_S             fk_SnT_T
    (SkillClass <----- Skill) <----- SkillNdFrTask -----> Task
    
We again make use of the `observation_equations` section to denormalize all of the attributes that 
are available through paths as attributes directly available to the `SkillNdFrTask` entity.
" *) }
schema sSTSpn = literal : TypeSide {
    entities
      Skill SkillClass
      Task
      SkillNdFrTask
    foreign_keys
      fk_HasClass    : Skill -> SkillClass
      fk_SkillNeeded : SkillNdFrTask -> Skill
      fk_ForTask     : SkillNdFrTask -> Task
    attributes
      SName         : Skill -> Varchar
      SCNameOnSkl   : Skill -> Varchar
      SCName        : SkillClass -> Varchar
      TName         : Task -> Varchar
      skill         : SkillNdFrTask -> Varchar
      skillclass    : SkillNdFrTask -> Varchar
      task          : SkillNdFrTask -> Varchar
    observation_equations
      SCNameOnSkl = fk_HasClass.SCName
      skill = fk_SkillNeeded.SName
      skillclass = fk_SkillNeeded.fk_HasClass.SCName
      task = fk_ForTask.TName
}

md { (* "
Given the similarity of the source and target schemas the entity sub-sections are all direct
mappings that reproduce exact copies of the source entities instances with those of the target
entities with the exception of the `SkllNdFrTask` target entity. In order to construct that entity 
we must have generators on both sides of the new foreign keys we are introducing. By default when
multiple source generator variables are provided for a target entity each generator for the 
target comes from the Cartesian product of the source generators. The where clause has two
equations implicitly connected by a logical and. This effectively constrains the target entities
to an equationally defined subset of a Cartesian product, also known as a join, so this 
construction effectively joins the three source entities and makes all of their generators
available for the creation of the new foreign keys. The foreign keys can now be assigned the
values for the `Skill` and `Task` generators based on the values in the `SkillNdFrTask` section.
" *) }
query qS_T_SnT_SumToSTSpn = literal : sS_T_SnT_Sum -> sSTSpn {
    entity
        Skill -> {
            from
                ss : lSkill
            attributes
                SName -> ss.lSName
                SCNameOnSkl -> ss.lSCNameOnSkl
               foreign_keys 
                      // fk_HasClass : Skill -> SkillClass
        fk_HasClass    -> {scsc -> ss.lfk_HasClass}
        }
    entity    SkillClass -> {
            from
                scsc : lSkillClass
            attributes
                SCName -> scsc.lSCName
                
        }
   entity     SkillNdFrTask -> {
            from
                snt : lSkillNeededForTask
                sn  : lSkill
                nt  : lTask
            where
                snt.lskill = sn.lSName
                snt.ltask = nt.lTName
            attributes
                skill -> snt.lskill
                skillclass -> sn.lfk_HasClass.lSCName
                task  -> snt.ltask
                foreign_keys
        // fk_SnT_S    : SkillNdFrTask -> Skill
        fk_SkillNeeded -> {ss -> sn}
        // fk_SnT_T    : SkillNdFrTask -> Task
        fk_ForTask     -> {tt -> nt}
        }
   entity     Task -> {
            from
                tt : lTask
            attributes
                TName -> tt.lTName
        }
    
}

md { (* "
Pushing the instance generated by the `coproduct_sigma` construction through the query above using
eval produces an instance reflecting the foreign keys introduced in the target schema we set up 
as part of the query definition.
" *) }
instance iSTSpn = eval qS_T_SnT_SumToSTSpn iS_T_SnT_Sum


md { (* "
---

## JDBC Pragmas, Imports and Exports

This first sequence exports the `iSTSpn` created above. Exporting a JDBC instance consolidates
several sets of commands. First the export issues data definition (DDL) commands against the
target RDBMS that create tables and foreign keys that are as close as possible to the associated
CQL schema kind. The second sequence of commands inserts instance records into the tables just 
created. Since this file can be executed several times we must first check to see if the table
structure already exists and if so drop those tables. This task is performed using the pragma
`pCleanUp`. In order to display the results of these commands the set of tables in the target schema
are listed using the `pShwTbls` pragma.

First clean out the target tables if present from a previous run.
" *) }
command pCleanUp = exec_jdbc "com.mysql.cj.jdbc.Driver"
"jdbc:mysql://mysql.catinf.com:3306/ci_test?user=catinfcom&password=FDiUY!pr&serverTimezone=America/New_York" {
"DROP TABLE IF EXISTS sts_SkillNdFrTask"
"DROP TABLE IF EXISTS sts_Skill"
"DROP TABLE IF EXISTS sts_SkillClass"
"DROP TABLE IF EXISTS sts_Task"}

md { (* "
Export the results of the above construction of the span and its representative instance. The
`export_jdbc_instance` defaults strings varchar(64). In this case we illustrate how that default 
can be overridden using the varchar_length option.
" *) }
command eSTS = export_jdbc_instance iSTSpn "com.mysql.cj.jdbc.Driver"
"jdbc:mysql://mysql.catinf.com:3306/ci_test?user=catinfcom&password=FDiUY!pr&serverTimezone=America/New_York" "sts_"
  { options varchar_length = 256 }

md { (* "
Display the contents of the `ci_test` schema in MySQL.
" *) }
command pShwTbls = exec_jdbc "com.mysql.cj.jdbc.Driver"
"jdbc:mysql://mysql.catinf.com:3306/ci_test?user=catinfcom&password=FDiUY!pr&serverTimezone=America/New_York" {
"SHOW  TABLES"}
 
md { (* "
The default CQL import process from a relational database submits an SQL query through the JDBC
driver for the source RDBMS for each entity declared in the target schema `sSTSpn` above. Due to
the extensive differences between CQL and RDBMS semantics, the translation process has a number
of different options that can be used to provide flexibility. We make use of the default behavior
for this import but will return to import options in a later example. The import defaults require
a complete specification of all foreign key and attribute columns.
" *) }
instance iSTSpnImp = import_jdbc "com.mysql.cj.jdbc.Driver"
"jdbc:mysql://mysql.catinf.com:3306/ci_test?user=catinfcom&password=FDiUY!pr&serverTimezone=America/New_York" : sSTSpn {
   Skill -> "select id, fk_HasClass, SName, SCNameOnSkl from sts_Skill"
   SkillClass -> "select id, SCName from sts_SkillClass"
   Task -> "select id, TName from sts_Task"
   SkillNdFrTask -> "select id, fk_SkillNeeded, fk_ForTask, skill, task, skillclass from sts_SkillNdFrTask"
//options
  // always_reload = true -- why?
}

md { (* "
Having constructed a span modeling the way skills are related to tasks, we now turn to another
model involving a span relating skills to people. We could build this model up piece by piece as
we did above, but CQL's schema declaration can build such a schema in one step. To illustrate a
more elaborate example of CQL's integration capabilities the set of tables used to model skills 
for this schema has an additional level to the taxonomy. The creation of this schema is a
precondition for the upcoming import. In order to extract existing data from a relational 
database into an CQL schema we must first define an CQL schema that is compatible with the
external SQL system. The schema below defines the structure into which we target our import.
" *) }
schema sSPSpn = literal : TypeSide {
    entities
      SpecificSkill GeneralSkill SkillClass
      Person SkillPrvdByPerson
    foreign_keys
      fk_IsType : SpecificSkill -> GeneralSkill
      fk_HasClass : GeneralSkill -> SkillClass
      fk_SpP_S   : SkillPrvdByPerson -> SpecificSkill
      fk_SpP_P   : SkillPrvdByPerson -> Person
    attributes
      SSName SSGSName SSSCName : SpecificSkill -> Varchar
      GSName GSSCName : GeneralSkill -> Varchar
      SCName : SkillClass -> Varchar
      PName  : Person -> Varchar
      skill  : SkillPrvdByPerson -> Varchar
      person : SkillPrvdByPerson -> Varchar
    observation_equations
      SSGSName = fk_IsType.GSName
      SSSCName = fk_IsType.fk_HasClass.SCName
      GSSCName = fk_HasClass.SCName
      skill = fk_SpP_S.SSName
      person = fk_SpP_P.PName
}

md { (* "
We have already set up a set of tables and associated instance in our source RDBMS so we import
that instance using a command analogous to the `iSTSpnImp` imported instance.
" *) }
instance iSPSpnImp = import_jdbc "com.mysql.cj.jdbc.Driver"
    "jdbc:mysql://mysql.catinf.com:3306/ci_test?user=catinfcom&password=FDiUY!pr&serverTimezone=America/New_York" : sSPSpn {
   SpecificSkill -> "select id, fk_IsType, SSName, SSGSName, SSSCName from sps_SpecificSkill"
   GeneralSkill -> "select id, fk_HasClass, GSName, GSSCName from sps_GeneralSkill"
   SkillClass -> "select id, SCName from sps_SkillClass"
   Person -> "select id, PName from sps_Person"
   SkillPrvdByPerson -> "select id, fk_SpP_S, fk_SpP_P, skill, person from sps_SkillPrvdByPerson"
//options
  // always_reload = true - why?
}

md { (* "
In this example we begin to see for the first time a more representative example of the full 
capability of the `schema_colimit` construction. In this more interesting colimit we are gluing 
(equating) on a defined overlap that is associated with the slightly different strategies for 
modeling skill taxonomies. In this case we are assuming that the Skill entity from `sSTSpn` can be
equated with the `SpecificSkill` entity from `sSPSpn` and likewise the `SkillClass` entities from both. 
We also note what this implies about foreign keys. Since `SpecificSkill` has a foreign key path 
through `GeneralSkill` we equate that path with the foreign key `fk_HasClass` from `sSTSpn`. In so 
doing we consolidate the two schemas in a way that eliminates redundant entities and paths.

Generally this is what a colimit does. It merges multiple objects of a category into a single
object in a way that equates all paths from objects in the colimit diagram (in this case schemas)
to the universal colimit (in this case the `schema_colimit`). These path equalities (in this case 
the pair of paths from a link schema through `sSTSpn` and `sSPSpn` to `sSTSpn_SPSpn_CoLim`) serve to 
define ways in which structure is merged across the objects in the colimit diagram. Although this
approach is supported by CQL there is an equivalent alternative. 

One particularly intuitive way to implement a colimit involves deconstructing the colimit into 
two steps. The first step is to take the coproduct. The coproduct is a special case of a colimit 
where the colimit diagram consists of a set of discrete objects without any connecting arrows. 
With such a diagram the colimit assembles the source objects disjointly. The second step is the 
quotient which provides for the merging of structure across and within the source objects. What 
is the structure being merged? It is the structure of a category, which consists of objects and 
paths.

Recall the comparison we illustrated between the use of path equations in the instance 
declaration associated with `iSklSklCls1` and the use of a link instance and pair of transforms 
used in the construction of `isklSklCls2`. Similarly the following sequence follows the sequence
you have seen before where we disjointly assembled `sS`, `sT` and `sSnT`. In that sequence we followed
with a query to create foreign keys. In this sequence we are doing something different. Instead
of joining a set of disjoint entities we are gluing a pair of schema with some shared structure.
" *) } 
schema_colimit STSpn_SPSpn_CoLimAuto = quotient sSTSpn + sSPSpn : TypeSide {
    entity_equations
      sSTSpn.Skill = sSPSpn.SpecificSkill
      sSTSpn.SkillClass = sSPSpn.SkillClass
    path_equations
      sSPSpn_SpecificSkill__sSTSpn_Skill.sSTSpn_Skill_fk_HasClass = sSPSpn_SpecificSkill__sSTSpn_Skill.sSPSpn_SpecificSkill_fk_IsType.sSPSpn_GeneralSkill_fk_HasClass
//    observation_equations
//      forall s. s.sSTSpn_SName = s.sSPSpn_SName
//      forall c. c.sSTSpn_SCName = c.sSPSpn_SCName
}

md { (* "
As we did in the construction of `S_T_SnT_Sum` we can relabel components in the colimit schema 
using the modify command. An important point to notice is that attributes coming from different
source schemas must be maintained as distinct attributes in the colimit. This must be done even
though conceptually they may duplicate one another. This is essential as there is no guarantee
that different sources will be consistent in labeling equivalent records.
" *) }
schema_colimit STSpn_SPSpn_CoLim = modify STSpn_SPSpn_CoLimAuto {
    rename entities
        sSPSpn_GeneralSkill -> GeneralSkill
        sSPSpn_Person -> Person
        sSPSpn_SkillClass__sSTSpn_SkillClass -> SkillClass
        sSPSpn_SkillPrvdByPerson -> SkillPrvdByPerson
        sSPSpn_SpecificSkill__sSTSpn_Skill -> SpecificSkill
        sSTSpn_SkillNdFrTask -> SkillNdFrTask
        sSTSpn_Task -> Task
    rename foreign_keys
        SkillNdFrTask. sSTSpn_SkillNdFrTask_fk_SkillNeeded -> fk_SkillNeeded
        SkillNdFrTask. sSTSpn_SkillNdFrTask_fk_ForTask -> fk_ForTask
        SkillPrvdByPerson. sSPSpn_SkillPrvdByPerson_fk_SpP_S -> fk_SpP_S
        SkillPrvdByPerson. sSPSpn_SkillPrvdByPerson_fk_SpP_P -> fk_SpP_P
        SpecificSkill. sSPSpn_SpecificSkill_fk_IsType -> fk_IsType 
        GeneralSkill. sSPSpn_GeneralSkill_fk_HasClass -> fk_HasClass
    rename attributes
        SkillNdFrTask.sSTSpn_SkillNdFrTask_skillclass -> ST_SCNameOnSnT
        SkillClass.sSPSpn_SkillClass_SCName -> SP_SCName
        SkillNdFrTask.sSTSpn_SkillNdFrTask_task -> ST_TNameOnSnT
        SkillPrvdByPerson.sSPSpn_SkillPrvdByPerson_skill -> ST_SNameOnSnT
        SpecificSkill.sSTSpn_Skill_SName -> ST_SklName
        Task.sSTSpn_Task_TName -> ST_TName
        SkillClass.sSTSpn_SkillClass_SCName -> ST_SCName
        SpecificSkill.sSTSpn_Skill_SCNameOnSkl -> ST_SCNameOnSS
        SpecificSkill.sSPSpn_SpecificSkill_SSGSName -> SP_GSNameOnSS
        SpecificSkill.sSPSpn_SpecificSkill_SSName -> SP_SSName
        SpecificSkill.sSPSpn_SpecificSkill_SSSCName -> SP_SCNameOnSS
        SkillPrvdByPerson.sSPSpn_SkillPrvdByPerson_person -> SP_PNameOnSnP
        SkillNdFrTask.sSTSpn_SkillNdFrTask_skill -> SP_SNameOnSnP
        Person.sSPSpn_Person_PName -> SP_PName
        GeneralSkill.sSPSpn_GeneralSkill_GSSCName -> SP_SCNameOnGS
        GeneralSkill.sSPSpn_GeneralSkill_GSName -> SP_GSName
    remove foreign_keys    
        SpecificSkill.sSTSpn_Skill_fk_HasClass -> fk_IsType.fk_HasClass
}

md { (* "
Provide addressable labels for the various components of the `schema_colimit` `STSpn_SPSpn_CoLim`.
" *) }
schema sSTSpn_SPSpn_CoLim = getSchema STSpn_SPSpn_CoLim
mapping mSTSpnToSTSpn_SPSpn_CoLim = getMapping STSpn_SPSpn_CoLim sSTSpn
mapping mSPSpnToSTSpn_SPSpn_CoLim = getMapping STSpn_SPSpn_CoLim sSPSpn

md { (* "
Again as we did with the instances coming from the disjoint schemas `sS`, `sT`, and `sSnT`, we must deal 
with the instance data coming from the two imports. We use `coproduct_sigma` to land those two
instances into the assembled `colimit_schema` of the two sources schemas `sSTSpn` and `sSPSpn`.
" *) }

instance AA = sigma mSTSpnToSTSpn_SPSpn_CoLim iSTSpnImp
instance BB = sigma mSPSpnToSTSpn_SPSpn_CoLim iSPSpnImp
instance iST_SP_Sum = literal : sSTSpn_SPSpn_CoLim {
	imports AA BB
}
   
md { (* "
In order to make instance assembly clear we illustrate two equivalent methods. The second method 
has the same preconditions as the first but performs the record linking transparently as part of
the import process. 

Merging records by importing a link table and transforms from tables constructed externally.

In this sequence we create external tables using SQL statements against the tables generated in
the export. The essence of this approach is matching records based on attributes. Clearly this
will only work if we can get exact string matches. This process can be generalized using any
number of technical enhancements to include fuzzing matching using things like edit distance or
matching through machine learning approaches.

As is well known reliable integration depends on standardization and/or investment in the
construction of translations between systems that make use of different reference information,
e.g. skill classification in this case.
" *) }

instance iSTSpn_SPSpn_Merge2 = quotient_query iST_SP_Sum {
	

}
/*
// Set up the SkillMatch and SkillClassMatch tables based on the exports.
command pSetUpSkillMatch = exec_jdbc "com.mysql.cj.jdbc.Driver"
"jdbc:mysql://mysql.catinf.com:3306/ci_test?user=catinfcom&password=FDiUY!pr&serverTimezone=America/New_York" {
"DROP TABLE IF EXISTS SkillMatch"
"DROP TABLE IF EXISTS SkillClassMatch"

"create table SkillMatch as
select
  concat('s', t.id) as sm_id,
  p.id as ps_id,
  t.id as ts_id,
  t.fk_HasClass
from
  sps_SpecificSkill p
  join sts_Skill t
    on p.SSName = t.SName
  join sps_GeneralSkill gs
    on p.fk_IsType = gs.id"

"create table SkillClassMatch as
select
  concat('sc', t.id) as scm_id,
  p.id as psc_id,
  t.id as tsc_id
from
  sps_SkillClass as p
  join sts_SkillClass as t
    on p.SCName = t.SCName"
}

*/
 
 
md { (* "
Prior to wrapping up let's return to an examination of the data migration functors sigma, delta 
and pi as well as their connection with querying. We mentioned before that the `coproduct_sigma` 
could be constructed into first the migration of instances separately into the colimit schema and 
then taking the coproduct of each result. Let's examine what some of those intermediate steps do.
" *) }
instance iSTSpnImpInCoLim = sigma mSTSpnToSTSpn_SPSpn_CoLim iSTSpnImp

md { (* "
As you can see by inspecting the runtime result we have loaded the instance `iSTSpnImp` into the
assembled schema we created using the colimit. Since there are entities in the colimit schema
not present in `sSTSpn`. You will notice that some entities are empty; in particular, those 
entities that arose from `sSPSpn`. There are also Skolem terms that are generated due to the 
introduction `GeneralSkill` referenced by `SpecialSkill` not present in `sSTSpn`.

Let's close the loop using delta right adjoint to sigma. We present the instance generated by
sigma above to delta at the same map. We have pushed `iSTSpnImp` forward through sigma and then
pulled the result back through the same schema map.
" *) }

instance iSgmDltSTSpnImp = delta mSTSpnToSTSpn_SPSpn_CoLim iSTSpnImpInCoLim

md { (* "
We see an instance that looks the same as `iSTSpnImp` originally imported. Intuitively we can think 
of this as delta projecting the part of the colimit schema instance back into the schema from 
which it came. It should not be surprising that this would yield the identity. This can be 
verified by looking at the unit transform and comparing it to the identity transform.
" *) }
transform tUnit = unit mSTSpnToSTSpn_SPSpn_CoLim iSTSpnImp
transform tIdSTSpnImp = identity iSTSpnImp

md { (* "
The `SkillClass` transform output is not immediately obviously the identity, but if you unravel the 
provenance you will find that it is. Start by looking at instance `iSTSpnImp` and you will see for
entity Skill:

    ID     SCNameOnSkl    fk_HasClass
    ---    -----------    -----------
    100    Cognitive      122
    ...    ...            ...
    102    Social         120
    ...    ...            ...
    105    Psychomotor    121

Now look at the result of the quotient on the schema colimit in any of the three instances above, 
say `iSTSpn_SPSpn_Merge3`. Specifically the `SkillClass` entity:

    ID                             ST_SCName
    ---------------------------    -----------
    [100.fk_IsType.fk_HasClass]    Cognitive
    [102.fk_IsType.fk_HasClass]    Social
    [105.fk_IsType.fk_HasClass]    Psychomotor

Finally look at the instance `iSgmDltSTSpnImp` and you will see for `SkillClass`:

    ID                                         ST_SCName
    ---------------------------------------    -----------
    <SkillClass[100.fk_IsType.fk_HasClass]>    Cognitive
    <SkillClass[102.fk_IsType.fk_HasClass]>    Social
    <SkillClass[105.fk_IsType.fk_HasClass]>    Psychomotor

We can conclude:

    <SkillClass[100.fk_IsType.fk_HasClass]>    -> 122
    <SkillClass[102.fk_IsType.fk_HasClass]>    -> 120
    <SkillClass[105.fk_IsType.fk_HasClass]>    -> 121 

Let's try the other direction. We start with the merged instance in the schema colimit and use 
delta to project back through the mapping that inserted the schema `sSTSpn` into the schema 
colimit. As you would expect you loose almost all of the data that was supplied by `iSPSpnImp`. We 
now have 11 skills rather than the 10 skills provided by `iSTSpnImp` and so have picked up an 
additional Skolem term due to the \"dancing\" skill that came from `iSPSpnImp`.
" *) }
instance iCoLimInSTSpn = delta mSTSpnToSTSpn_SPSpn_CoLim iSTSpn_SPSpn_Merge2

md { (* "
Sigma then pushes this projected instance back into the schema colimit and we end up with an
instance that looks like the one sigma pushed forward from `iSTSpnImp` but with the extra skill
record. The round trip has eliminated all traces of `iSTSpn_SPSpn_Merge3` that came from `iSPSpnImp`
except the record that additional \"Dancing\" skill that is now Skolemized.
" *) }
instance iDltSgmCoLim = sigma mSTSpnToSTSpn_SPSpn_CoLim iCoLimInSTSpn
transform tCounit = counit mSTSpnToSTSpn_SPSpn_CoLim iSTSpn_SPSpn_Merge2

md { (* "
The counit transform injects `iDltSgmColim` back into the merged instance `iSTSpn_SPSpn_Merge3`.
" *) }

/*
md { (* "
Additional work that could be added to this tutorial in a later release:
 
  1. Examples of how relational nulls are handled when imported into CQL. This could be done
     easily by creating an alternative to `iS_T_SnT_Sum` by replacing `iSklSklCls1` with `iSkl`.
     Following that repeat all dependent constructions to see how null values appear on export
     and then are translated when imported.
     
  2. Follow the discussion above with additional constructions illustrating the connections
     between sigma, delta and pi and eval and coeval.
     
  3. Develop some examples of how one can use uber flowers to create data warehouse marts.

" *) }
*/

html { (* "
</body></html>" *) }	


