#pragma once

/** Overload for the stream operator for composition of java Strings */
java::lang::String operator<<(java::lang::String left, java::lang::String right){
  return __rt::literal((left->data + right->data).c_str());
}


