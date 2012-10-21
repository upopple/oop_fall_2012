/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2012 Robert Grimm
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
 * USA.
 */
package qimpp;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.PrintWriter;
import java.util.HashMap;

import xtc.lang.JavaFiveParser;

import xtc.parser.ParseException;
import xtc.parser.Result;

import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Visitor;
import xtc.tree.Location;
import xtc.tree.Printer;

import xtc.util.Tool;

/**
 * A translator from (a subset of) Java to (a subset of) C++.
 *
 * @author QIMPP
 * @version $Revision$
 */
public class CPPAST {
  GNode compilationUnit;
  HashMap<String, GNode> classesMap; 
  
  public CPPAST(){
      compilationUnit = GNode.create("CompilationUnit");
      GNode structs = GNode.create("Structs");
      GNode classes = GNode.create("Classes");
      compilationUnit.addNode(structs);
      compilationUnit.addNode(classes);
      classesMap = new HashMap<String, GNode>();
  }
  
  /*Methods to add:
   *GNode addClass(String name) - adds a class and a struct to the tree returns a GNode to the class
   *GNode addField(String name, String type, GNode class) - adds a field to a class returns the GNode of the field
   *GNode addMethod(String name, String returnType, GNode class) - adds a method to a class returns the method GNode
   *GNode addMethodInstruction(GNode instruction, GNode method) - adds instruction to method
   *GNode addMethodParameter(String paramType, String param, GNode method) - adds a parameter to method
   *
   *GNode getClass(String name) - gets a GNode to a class by it's name
   *GNode getMethod(String name, GNode* class) - gets a GNode to a method by it's name
   *GNode getField(String name, GNode* class) - gets a GNode to a Field by it's name
   *
   *void removeMethod(String name, GNode* class) - removes a method from a class
   
   *void printAST() - prints the AST for debugging
  */
  
  GNode addClass(String name){
    //Add to Structs
    compilationUnit.getGeneric(0).addNode(GNode.create("Struct")).add(name);
    
    //Add to Classes with Name node, Fields node, ImplementedMethods node, and InheritedMethods node
    GNode classNode = GNode.create("ClassDeclaration");
    classNode.add(name);
    classNode.addNode(GNode.create("Fields"));
    classNode.addNode(GNode.create("ImplementedMethods"));
    classNode.addNode(GNode.create("InheritedMethods"));
    compilationUnit.getGeneric(1).addNode(classNode);
    return classNode;
  }
  
  //Adding, getting, and removing fields
  
  GNode addField(String name, String type, GNode classNode){
    //Get the fields node
    GNode fieldNode = GNode.create("FieldDeclaration");
    fieldNode.add(name);
    fieldNode.addNode(GNode.create("Type")).add(type);
    classNode.getGeneric(1).addNode(fieldNode);
    return fieldNode;
  }
  
  GNode addMethod(String name, String returnType, GNode classNode) {
    GNode methodNode = GNode.create("MethodDeclaration");
    methodNode.add(name);
    methodNode.addNode(GNode.create("ReturnType").add(returnType));
    methodNode.addNode(GNode.create("FormalParameters"));
    methodNode.addNode(GNode.create("Block"));
    classNode.getGeneric(2).addNode(methodNode);
    return methodNode;
  }
  
  GNode addMethod(String name, String returnType, GNode classNode, String from) {
    GNode methodNode = GNode.create("MethodDeclaration");
    methodNode.add(name);
    methodNode.addNode(GNode.create("ReturnType").add(returnType));
    methodNode.add(GNode.create("FormalParameters"));
    methodNode.addNode(GNode.create("From")).add(from);
    classNode.getGeneric(3).addNode(methodNode);
    return methodNode;
  }
  
  GNode addMethodInstruction(GNode instruction, GNode method) {
    return method.getGeneric(3).addNode(instruction);
  }
  
  GNode addMethodParameter(String paramType, String param, GNode method) {
    GNode formalParameter = method.get(2).addNode("FormalParameter");
    formalParameter.add(name);
    formalParameter.addNode("Type").add(paramType);
    return formalPatameter;
  }
  
  GNode getClass(String name) {
      return classesMap.get(name);
  }
  
  //Returns the index of a field with name in the class given by classNode
  int getFieldIndex(String name, GNode classNode){
    GNode fieldsOfClass = classNode.getGeneric(1);
    for(int i = 0; i < fieldsOfClass.size(); i++){
      if(getGNodeName(fieldsOfClass.getGeneric(i)).equals(name))
        return i;
    }
    return -1;
  }
  
  void removeField(String name, GNode classNode){
    int fieldIndex = getFieldIndex(name, classNode);
    if(fieldIndex != -1) classNode.getGeneric(1).remove(fieldIndex);
  }
  
  
  //Adding, getting, and removing methods  
  int getInheritedMethodIndex(String name, GNode classNode) {
    GNode inheritedMethods = classNode.getGeneric(3);
    for(int i=0; i < inheritedMethods.size(); i++){
      if(getGNodeName(inheritedMethods.getGeneric(i)).equals(name))
        return i;
    }
    return -1;      
  }
  
  
  void removeInheritedMethod(String name, GNode classNode) {
    int methodIndex = getMethodIndex(name, classNode);
    if(methodIndex != -1) classNode.getGeneric(3).remove(fieldIndex);
  }
  //Utility methods
  
  String getGNodeName(GNode n){
    return n.getString(0);
  }
  
  public void printAST(){
    Printer p = new Printer(System.out);
    p.format(compilationUnit).flush();
  }
  
  public static void main(String args[]){
    CPPAST testAST = new CPPAST();
    
    GNode node1 = testAST.addClass("node1");
    testAST.addField("afield", "int", node1);
    testAST.addClass("node2");
    testAST.printAST();
  }
}