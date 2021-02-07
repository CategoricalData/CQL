Modifies a schema colimit by a sequence of entity and column renamings and removals.  A mapping name to name to  specifying a renaming of entities, foreign keys, and attributes.  Then, mapping name to path specifying that the foreign key named by name can be deleted by replacing all occurrences of it by the given path.  CQL checks if the removal is semantics-preserving. 

Finally a mapping name to lambda x. term specifying that the attribute name can be deleted by replacing all occurrences of it by the given term.  CQL checks if the removal is semantics-preserving. 
