package qimpp;

import java.util.*;
import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Visitor;
import xtc.tree.Printer;

import xtc.util.Tool;

/**
 * Mangles Block Nodes into better Block nodes for ImplementationPrinter
 *
 * @author QIMPP
 */

public class BlockMangler {

  public GNode cppClass;
  public InheritanceTreeManager inheritanceTree;
  public MethodResolver methodResolver;
 
  /** BlockMangler constructor. */ 
  public BlockMangler(
                GNode cppClass, 
                InheritanceTreeManager itm,
                MethodResolver mr
                ) {
    
    this.cppClass = cppClass;
    this.methodResolver = mr;
    this.inheritanceTree = itm;
  }
 
  // replaces nOld GNode with nNew GNode
  public void copyloc(GNode nOld, GNode nNew) {
    nNew.setLocation(nOld);
  }

  // takes java block 
  public GNode mangle(GNode java) {

    if (java.getProperty("Mangled") != null){
      (new Exception()).printStackTrace(System.err);
      System.exit(1);
    }
    java.setProperty("Mangled", new Boolean(true));

    
    // Vivek: it doesn't seem like this is being used.
    GNode cpp = GNode.create("Block");

    /**
     * The visitor that analyzes and modifies the block
     */
    new Visitor() {

      /** 
       * Determine if this is a class, a stackvar, field or the start of a fully
       * qualified class name and set the proper properties of the node 
       */
      public String visitPrimaryIdentifier(GNode n){
        String identifier = n.getString(0);

        if (identifier.equals("R1")){
          
          resolveClassField(identifier);
        }

        if (selectionExpressionBuilder != null){
          selectionExpressionBuilder.insert(0, identifier);
        }
        
        GNode classDeclaration = 
          inheritanceTree.getClassDeclarationNode(identifier);
        GNode stackVar = resolveScopes(n);
        GNode classField = resolveClassField(identifier);

        //
        //

        if (classDeclaration != null) {
          n.setProperty(Constants.IDENTIFIER_TYPE, Constants.QUALIFIED_CLASS_IDENTIFIER);
          n.setProperty(Constants.IDENTIFIER_DECLARATION, classDeclaration);
          n.setProperty(Constants.IDENTIFIER_TYPE_NODE,
              GNode.create("Type",
                Disambiguator.disambiguate(classDeclaration.getString(0)), null));
          return Constants.CLASS_IDENTIFIER;
        }

        else if (stackVar != null){
          n.setProperty(Constants.IDENTIFIER_TYPE, Constants.STACKVAR_IDENTIFIER);
          n.setProperty(Constants.IDENTIFIER_DECLARATION, stackVar);
          n.setProperty(Constants.IDENTIFIER_TYPE_NODE, stackVar.getGeneric(1)); 
          return Constants.STACKVAR_IDENTIFIER; 
        }

        else if (classField != null){
          n.setProperty(Constants.IDENTIFIER_TYPE, Constants.FIELD_IDENTIFIER);
          n.setProperty(Constants.IDENTIFIER_DECLARATION, classField);
          n.setProperty(Constants.IDENTIFIER_TYPE_NODE, classField.getGeneric(1));
          n.setProperty("class", Type.getClassTypeName(cppClass.getString(0)));

          // Set the value of the reference to the value of the field declaration
          n.set(0, classField.getString(0));

          return Constants.FIELD_IDENTIFIER;
        }

        // It must be a fully qualified class
        else {

          n.setProperty(Constants.IDENTIFIER_TYPE, Constants.QUALIFIED_CLASS_IDENTIFIER);
          n.setProperty(Constants.IDENTIFIER_DECLARATION, null);
          n.setProperty(Constants.IDENTIFIER_TYPE_NODE, null);
          return Constants.QUALIFIED_CLASS_IDENTIFIER;

        }

      }

      public String visitBooleanLiteral(GNode n) {
        n.setProperty(Constants.IDENTIFIER_TYPE, Constants.PRIMITIVE_TYPE_IDENTIFIER);
        n.setProperty(Constants.IDENTIFIER_TYPE_NODE, GNode.create("Type", GNode.create("PrimitiveType", "boolean"), null));
        return Constants.PRIMITIVE_TYPE_IDENTIFIER;
      }

      /**
       * Set the appropriate properties for an IntegerLiteral
       */
      public String visitIntegerLiteral(GNode n){
        n.setProperty(Constants.IDENTIFIER_TYPE, 
            Constants.PRIMITIVE_TYPE_IDENTIFIER);
        //TODO: Handle longs
        if (n.getString(0).charAt(n.getString(0).length()-1) == 'l' ||
            (n.getString(0).charAt(n.getString(0).length()-1) == 'L')) {
          n.setProperty(Constants.IDENTIFIER_TYPE_NODE, 
              GNode.create("Type", GNode.create("PrimitiveType", "long"), null));
        } else {
          n.setProperty(Constants.IDENTIFIER_TYPE_NODE, 
              GNode.create("Type", GNode.create("PrimitiveType", "int"), null));
        }
        return Constants.PRIMITIVE_TYPE_IDENTIFIER;
      }
      
      /**
       * Set the appropriate properties for a flp literal
       */
      public String visitFloatingPointLiteral(GNode n){
        n.setProperty(Constants.IDENTIFIER_TYPE, Constants.PRIMITIVE_TYPE_IDENTIFIER);
        //TODO: Handle float
        n.setProperty(Constants.IDENTIFIER_TYPE_NODE, GNode.create("Type", GNode.create("PrimitiveType", "double"), null));
        return Constants.PRIMITIVE_TYPE_IDENTIFIER;
      }

      /**
       * Set the appropriate properties for a char literal
       */
      public String visitCharacterLiteral(GNode n){
        n.setProperty(Constants.IDENTIFIER_TYPE, Constants.PRIMITIVE_TYPE_IDENTIFIER);
        n.setProperty(Constants.IDENTIFIER_TYPE_NODE, GNode.create("Type", GNode.create("PrimitiveType", "char"), null));
        return Constants.PRIMITIVE_TYPE_IDENTIFIER;
      }

      /**
       * Set the appropriate properties for a string literal
       */
      public String visitStringLiteral(GNode n){
        n.setProperty(Constants.IDENTIFIER_TYPE, Constants.CLASS_IDENTIFIER);
        n.setProperty(Constants.IDENTIFIER_DECLARATION, inheritanceTree.getClassDeclarationNode("java.lang.String"));
        n.setProperty(Constants.IDENTIFIER_TYPE_NODE, GNode.create("Type", Disambiguator.disambiguate("java.lang.String"), null));
        return Constants.CLASS_IDENTIFIER;
      }

      public String visitMultiplicativeExpression(GNode n){
        // A multiplicative expression always returns a primitive type
        n.setProperty(Constants.IDENTIFIER_TYPE, Constants.PRIMITIVE_TYPE_IDENTIFIER);
         
        dispatch(n.getGeneric(0));
        dispatch(n.getGeneric(2));
        
        String leftType = ((GNode)n.getGeneric(0).getProperty(Constants.IDENTIFIER_TYPE_NODE)).getGeneric(0).getString(0);

        String rightType = ((GNode)n.getGeneric(2).getProperty(Constants.IDENTIFIER_TYPE_NODE)).getGeneric(0).getString(0);

        String resultType = Type.compare(leftType, rightType);
        n.setProperty(Constants.IDENTIFIER_TYPE_NODE, 
            GNode.create("Type", 
              GNode.create("PrimitiveType", resultType), null));

        return Constants.PRIMITIVE_TYPE_IDENTIFIER;
      }

      public String visitAdditiveExpression(GNode n){

        if (n.get(0) instanceof String || n.get(2) instanceof String) {
          n.setProperty(Constants.IDENTIFIER_TYPE, Constants.CLASS_IDENTIFIER);
          n.setProperty(Constants.IDENTIFIER_DECLARATION, 
              inheritanceTree.getClassDeclarationNode("java.lang.String"));
          n.setProperty(Constants.IDENTIFIER_TYPE_NODE, 
              GNode.create("Type", 
                Disambiguator.disambiguate("java.lang.String"), null));
          return Constants.CLASS_IDENTIFIER;
        }
  
        dispatch(n.getGeneric(0));
        dispatch(n.getGeneric(2));

        GNode leftTypeNode = 
          (GNode) n.getGeneric(0).getProperty(Constants.IDENTIFIER_TYPE_NODE);
        GNode rightTypeNode = 
          (GNode)n.getGeneric(2).getProperty(Constants.IDENTIFIER_TYPE_NODE);

        if ((leftTypeNode != null && 
              leftTypeNode.getGeneric(0).getName().equals("QualifiedIdentifier"))
              || (rightTypeNode != null && 
                rightTypeNode.getGeneric(0).getName().equals("QualifiedIdentifier")))
        //if (n.getGeneric(0).getName().equals("QualifiedIdentifier") ||
        //    n.getGeneric(2).getName().equals("QualifiedIdentifier"))
        {
          n.setProperty(Constants.IDENTIFIER_TYPE, Constants.CLASS_IDENTIFIER);
          n.setProperty(Constants.IDENTIFIER_DECLARATION, 
              inheritanceTree.getClassDeclarationNode("java.lang.String"));
          n.setProperty(Constants.IDENTIFIER_TYPE_NODE, 
              GNode.create("Type", 
                Disambiguator.disambiguate("java.lang.String"), null));
          return Constants.CLASS_IDENTIFIER; 
        }
        
        n.setProperty(Constants.IDENTIFIER_TYPE, 
                      Constants.PRIMITIVE_TYPE_IDENTIFIER);

        if (null != leftTypeNode && null != rightTypeNode) {
          String leftType = leftTypeNode.getGeneric(0).getString(0);
          String rightType = rightTypeNode.getGeneric(0).getString(0);
          String resultType = Type.compare(leftType, rightType);
          n.setProperty(Constants.IDENTIFIER_TYPE_NODE, GNode.create("Type", 
                GNode.create("PrimitiveType", resultType), null));
        }
        
        return Constants.PRIMITIVE_TYPE_IDENTIFIER;    
      }

      

      
      public String visitThisExpression(GNode n){
        // Just to be consistent for ThisExpressions
        if (selectionExpressionBuilder != null){
          selectionExpressionBuilder.insert(0, "__this");
        }
        
        n.setProperty(Constants.IDENTIFIER_TYPE, Constants.CLASS_IDENTIFIER);
        n.setProperty(Constants.IDENTIFIER_DECLARATION, cppClass);
        n.setProperty(Constants.IDENTIFIER_TYPE_NODE, GNode.create("Type", Disambiguator.disambiguate(cppClass.getString(0)), null));

        return Constants.CLASS_IDENTIFIER;
      }

      private int selectionExpressionDepth = 0;
      private StringBuilder selectionExpressionBuilder;
      /**
       * Have the SelectionExpressions carry the innermost PrimaryIdentifier's type, except for the outermost one
       * for a qualified identifier
       */
      public String visitSelectionExpression(GNode n){
        if (selectionExpressionDepth == 0)
          selectionExpressionBuilder = new StringBuilder();

        selectionExpressionDepth++;
        n.setProperty(Constants.IDENTIFIER_TYPE, dispatch(n.getGeneric(0)));
        
        selectionExpressionBuilder.append("." + n.getString(1));
        //TODO: Debug code
        if (n.getStringProperty(Constants.IDENTIFIER_TYPE) == null){
          
          throw new NullPointerException();
        }
        // End debug code
        n.setProperty(Constants.IDENTIFIER_DECLARATION, n.getGeneric(0).getProperty(Constants.IDENTIFIER_DECLARATION));
        
        n.setProperty(Constants.IDENTIFIER_TYPE_NODE, n.getGeneric(0).getProperty(Constants.IDENTIFIER_TYPE_NODE));
        

        selectionExpressionDepth--;
        String expression = selectionExpressionBuilder.toString();
        // Bug out if it's System.out
        if (expression.equals("System.out")) {
           n.setProperty(Constants.IDENTIFIER_TYPE, Constants.PRINT_IDENTIFIER);
           //TODO:Hack
           n.setProperty(Constants.IDENTIFIER_DECLARATION, new Object());
           return Constants.PRINT_IDENTIFIER;
        }

        // Test if we're getting a field of ARRAY
        if (!(n.getStringProperty(Constants.IDENTIFIER_TYPE).equals(Constants.QUALIFIED_CLASS_IDENTIFIER) && n.getProperty(Constants.IDENTIFIER_DECLARATION) == null)){
          if (((GNode)n.getProperty(Constants.IDENTIFIER_TYPE_NODE)).getGeneric(1) != null){
            
            n.setProperty(Constants.IDENTIFIER_TYPE_NODE, GNode.create("Type", GNode.create("QualifiedIdentifier",
                                                                      "__rt", "Array"), null));
          }
        }

        // Part of the way in, we may find that we have a fully qualified type. In that case set the class declaration
        if (n.getStringProperty(Constants.IDENTIFIER_TYPE).equals(Constants.QUALIFIED_CLASS_IDENTIFIER) && n.getProperty(Constants.IDENTIFIER_DECLARATION) == null){
          
          GNode classDeclaration = inheritanceTree.getClassDeclarationNode(selectionExpressionBuilder.toString());
          if (classDeclaration != null){
            n.setProperty(Constants.IDENTIFIER_TYPE, Constants.QUALIFIED_CLASS_IDENTIFIER);
            n.setProperty(Constants.IDENTIFIER_DECLARATION, classDeclaration);
            n.setProperty(Constants.IDENTIFIER_TYPE_NODE, GNode.create("Type", Disambiguator.disambiguate(classDeclaration.getString(0)), null));
          }
        }
        else if (n.getStringProperty(Constants.IDENTIFIER_TYPE) == Constants.STACKVAR_IDENTIFIER){
          
          GNode foreignClass = inheritanceTree.getClassDeclarationNode(Disambiguator.getDotDelimitedName(
                ((GNode)n.getProperty(Constants.IDENTIFIER_TYPE_NODE)).getGeneric(0)));

          GNode foreignFieldDeclaration = resolveClassField(n.getString(1), foreignClass);
          
           String underscores = foreignClass.getString(0);
           underscores = underscores.replace('.', '_');

           n.set(1, underscores+"_"+n.getString(1));
           

           n.set(1, foreignFieldDeclaration.getString(0));


          n.setProperty(Constants.IDENTIFIER_TYPE, Constants.FOREIGN_CLASS_FIELD_IDENTIFIER);
          n.setProperty(Constants.IDENTIFIER_DECLARATION, foreignFieldDeclaration);
          n.setProperty(Constants.IDENTIFIER_TYPE_NODE, foreignFieldDeclaration.getGeneric(1));
        }

        // If our child is a CLASS_IDENTIFIER, and we're still in a SelectionExpression, we must be referring to some accessible field
        else if ((n.getStringProperty(Constants.IDENTIFIER_TYPE) == Constants.QUALIFIED_CLASS_IDENTIFIER
                  && n.getProperty(Constants.IDENTIFIER_DECLARATION) != null)
                 || n.getStringProperty(Constants.IDENTIFIER_TYPE) == Constants.CLASS_IDENTIFIER) {
           n.setProperty(Constants.IDENTIFIER_TYPE, Constants.FOREIGN_CLASS_FIELD_IDENTIFIER);
           GNode foreignFieldDeclaration = resolveClassField(n.getString(1), (GNode)n.getProperty(Constants.IDENTIFIER_DECLARATION));
           // Debug
           if (foreignFieldDeclaration == null) {
              
              
              throw new RuntimeException("Failed to identify field " + n.getString(1));
           }
           // Reset the field to its proper name

           String underscores = ((GNode)n.getProperty(Constants.IDENTIFIER_DECLARATION)).getString(0);
           underscores = underscores.replace('.', '_');

           n.set(1, underscores+"_"+n.getString(1));
           

           n.set(1, foreignFieldDeclaration.getString(0));

           n.setProperty(Constants.IDENTIFIER_DECLARATION, foreignFieldDeclaration);
           n.setProperty(Constants.IDENTIFIER_TYPE_NODE, foreignFieldDeclaration.getGeneric(1));
        }

        // If we're referring to some foreign class, we want to search it for this field's declaration
        else if (n.getStringProperty(Constants.IDENTIFIER_TYPE).equals(Constants.FOREIGN_CLASS_FIELD_IDENTIFIER)){
           GNode searchClassType = ((GNode)n.getProperty(Constants.IDENTIFIER_DECLARATION)).getGeneric(1);
           // Use the Type's QualifiedIdentifier's class
           String searchClassName = Disambiguator.getDotDelimitedName(searchClassType.getGeneric(0));          
           GNode searchClassDeclaration = inheritanceTree.getClassDeclarationNode(searchClassName);
           GNode fieldDeclaration = resolveClassField(n.getString(1), searchClassDeclaration);
           if (fieldDeclaration == null) {
              throw new NullPointerException();
           }

          String underscores = ((GNode)n.getProperty(Constants.IDENTIFIER_DECLARATION)).getString(0);
          underscores = underscores.replace('.', '_');

          n.set(1, underscores+"_"+n.getString(1));

          
           // Set the value of the reference to the value of the field declaration

           
           n.setProperty(Constants.IDENTIFIER_DECLARATION, fieldDeclaration);
           n.setProperty(Constants.IDENTIFIER_TYPE, fieldDeclaration.getGeneric(1));
        }
        
        

        // Bye this point we should have figured out what the selectionExpression is referring to
        if (selectionExpressionDepth == 0 && n.getProperty(Constants.IDENTIFIER_DECLARATION) == null){
          
          
          
          
          
          throw new RuntimeException("Selected unknown class or field!");
        }

        return n.getStringProperty(Constants.IDENTIFIER_TYPE);
      }

      public void visitInstanceOfExpression(GNode n) {
        String rightSide =
          Type.getClassTypeName(n.getGeneric(1).getGeneric(0));
        n.set(1, rightSide); 
        visit(n);
      }

      public String visitCastExpression(GNode n){
        visit(n);
        n.setProperty(Constants.IDENTIFIER_TYPE, Constants.CLASS_IDENTIFIER);
        GNode classDeclaration = inheritanceTree.getClassDeclarationNode(Disambiguator.getDotDelimitedName(n.getGeneric(0).getGeneric(0))); 
        n.setProperty(Constants.IDENTIFIER_DECLARATION, classDeclaration);
        n.setProperty(Constants.IDENTIFIER_TYPE_NODE, n.getGeneric(0));

        return Constants.CLASS_IDENTIFIER;
      }

      public String visitCallExpression(GNode n){
        visit(n);
        
        GNode caller = n.getGeneric(0);

        GNode callerType;
        String callType;


        if (caller != null){
          if (caller.getProperty(Constants.IDENTIFIER_TYPE) == Constants.PRINT_IDENTIFIER){
            //Ignore print expressions, they are evil :P
            return null;
          }
          callerType = (GNode)caller.getProperty(Constants.IDENTIFIER_TYPE_NODE);
          
          

          if (callerType.size() > 1 && null != callerType.getGeneric(1)) {
            GNode newQualifiedIdentifier = 
              GNode.create("QualifiedIdentifier", "__rt", "Array");
            GNode newTypeNode = GNode.create("Type", newQualifiedIdentifier, null);
            callerType = newTypeNode;
          }

          if (caller.getProperty(Constants.IDENTIFIER_TYPE) == Constants.QUALIFIED_CLASS_IDENTIFIER){
            callType = Constants.CALL_UNKNOWN;
          }
          // It must be a call from some object
          else {
            callType = Constants.CALL_UNKNOWN;
          }
        }
        else {
          callerType = GNode.create("Type", Disambiguator.disambiguate(cppClass.getString(0)), null);
          // We cannot know if this is a static or dynamic call, we need MethodResolver to determine that
          callType = Constants.CALL_UNKNOWN; 
        }
        GNode argumentTypes = GNode.create("ArgumentTypes");
        
        for ( Object o : n.getGeneric(3)) {
          
          
          argumentTypes.add(((GNode)o).getProperty(Constants.IDENTIFIER_TYPE_NODE));
        }



        GNode callInfo = null;
        try {
          
          
          
          
          
          callInfo = MethodResolver.resolve(n.getString(2), callerType, argumentTypes, inheritanceTree, callType, cppClass); 
        }
        catch (Exception e) {
          
          
          e.printStackTrace(System.err);
          System.exit(1);
        }

        // Rename the call
        n.set(2, callInfo.getString(0));

        GNode calledMethod = callInfo.getGeneric(2);
        n.setProperty("static",  calledMethod.getProperty("static"));
        n.setProperty("private", calledMethod.getProperty("private"));


        GNode returnType = callInfo.getGeneric(1);
        
        if (returnType.getGeneric(0).getName().equals("QualifiedIdentifier")){
          n.setProperty(Constants.IDENTIFIER_TYPE, Constants.CLASS_IDENTIFIER);
          n.setProperty(Constants.IDENTIFIER_DECLARATION,
              inheritanceTree.getClassDeclarationNode(Disambiguator
                .getDotDelimitedName(returnType.getGeneric(0))));
        }

        else {
          n.setProperty(Constants.IDENTIFIER_TYPE,
              Constants.PRIMITIVE_TYPE_IDENTIFIER);
        }

        n.setProperty(Constants.IDENTIFIER_TYPE_NODE, returnType);

        return n.getStringProperty(Constants.IDENTIFIER_TYPE);
      }

      public void visitDeclarator(GNode n) {
        if (null != n.getGeneric(1)) {
          GNode fakePrimary = GNode.create("PrimaryIdentifier", n.getString(0));
          fakePrimary.setProperty(Constants.SCOPE, 
              n.getProperty(Constants.SCOPE));
          GNode fieldNode = resolveScopes(fakePrimary);
          fieldNode.getGeneric(1).set(1, n.getGeneric(1));
        } 
        visit(n);
      }


      public void visitSubscriptExpression(GNode n){
        dispatch(n.getGeneric(0));
        GNode primaryIdentifierType =
          (GNode)n.getGeneric(0).getProperty(Constants.IDENTIFIER_TYPE_NODE);
        // Make a Declarators of one dimension lower
        GNode newDimensions = GNode.create("Dimensions");
        
        
        
        for (int i = 1; i < primaryIdentifierType.getGeneric(1).size(); i++)
          newDimensions.add("[");

        //If there are none, make it null
        if (newDimensions.size() == 0)
          newDimensions = null;

        GNode newTypeNode = GNode.create("Type", primaryIdentifierType.getGeneric(0), newDimensions);

        n.setProperty(Constants.IDENTIFIER_TYPE_NODE, newTypeNode);

        dispatch(n.getGeneric(1));
      }

      /**
       * Set the appropriate properties for a new class expression so it can be nested
       */
      public String visitNewClassExpression(GNode n) {
        GNode classType = n.getGeneric(2);
        
        dispatch(classType);
        
        n.setProperty(Constants.IDENTIFIER_TYPE,
            classType.getProperty(Constants.IDENTIFIER_TYPE));
        
        n.setProperty(Constants.IDENTIFIER_DECLARATION,
            classType.getProperty(Constants.IDENTIFIER_DECLARATION));
        
        n.setProperty(Constants.IDENTIFIER_TYPE_NODE,
            classType.getProperty(Constants.IDENTIFIER_TYPE_NODE));

        return n.getStringProperty(Constants.IDENTIFIER_TYPE);
      } 

      /* 
      public void tempVisitCallExpression(GNode n) {
        if (n.getGeneric(0).getGeneric(0).getString(0).equals("System")
                        && n.getGeneric(0).getString(1).equals("out")) {
          String option = (n.getString(2).equals("println")) ? " << endl" : null;  
          GNode printBody = dispatch(n.getGeneric(3));
        }

        cpp.add(GNode.create("PrintExpression", option, printBody));  
      }

      public void tempVisitArguments(GNode n) {
        GNode body = GNode.create("PrintBody");
        visit(n);
        for (Object o : n) {
          if (o instanceof Node) {
            body.add((GNode)o);
          } 
          else { 
            body.add((String)o); 
          }
        }
        cpp.add(body);
      }

      public void tempVisitAdditiveExpression(GNode n) {
        visit(n);
        GNode expr;
        left = dispatch(n.getGeneric(0));
        right = dispatch(n.getGeneric(2));
        if (getType(left) == getType(right)) {
          expr = GNode.create("AdditiveExpression", left, n.getString(1), right);
        }
        else {
          expr = GNode.create("ConcatExpression", left, "<<", right);
        }
        cpp.add(expr);
      }
      */


      public void visit(Node n) {
        for (Object o : n) if (o instanceof Node) dispatch((Node)o);
      } 

    }.dispatch(java);
    
    return null; 
  }

  public GNode getType(GNode n) {
    //declaration = n.getProperty(Constants.SCOPE).node();
    return null;
  }

  private GNode resolveScopes(GNode primaryIdentifier){
    SymbolTable.Scope scope = (SymbolTable.Scope)primaryIdentifier.getProperty(Constants.SCOPE);

    if (scope == null) {
      
      return null;
    }
    GNode result = (GNode)scope.lookup(primaryIdentifier.getString(0)); 
    
    if (result == null){ 
      
    }
    return result;
  }

  private GNode resolveClassField(String fieldName){
    GNode targetClass = cppClass;
    GNode fieldDeclaration = null;
    while (targetClass != null &&  !targetClass.getString(0).equals("java.lang.Object")){
      HashMap<String, GNode> fieldNameMap = (HashMap<String, GNode>)cppClass.getProperty("FieldMap");
      
      fieldDeclaration = fieldNameMap.get(fieldName);
      
      if (fieldDeclaration != null){
        fieldDeclaration.setProperty("ContainingClass", targetClass);
        return fieldDeclaration;
      }
      targetClass = (GNode)targetClass.getProperty("ParentClassNode");
    }
    return fieldDeclaration;
  }

  // Overload to refer to another class
  private GNode resolveClassField(String fieldName, GNode cppClass){
    HashMap<String, GNode> fieldNameMap = (HashMap<String, GNode>)cppClass.getProperty("FieldMap");
    GNode fieldDeclaration = fieldNameMap.get(fieldName);
    return fieldDeclaration;
  }



}
