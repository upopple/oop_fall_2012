/*
 * Object-Oriented Programming
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

#include <iostream>

#include "java_lang.h"

using namespace java::lang;

int main(void) {
  // Let's get started.
  std::cout << "--------------------------------------------------------------"
            << "----------------"
            << std::endl;

  // Object o = new Object();
  Object o = new __Object();

  std::cout << "o.toString() : "
            << o->__vptr->toString(o) // o.toString()
            << std::endl;

  // Class k = o.getClass();
  __rt::checkNotNull(o);
  Class k = o->__vptr->getClass(o);

  __rt::checkNotNull(k);
  std::cout << "k.getName()  : "
            << k->__vptr->getName(k) // k.getName()
            << std::endl
            << "k.toString() : "
            << k->__vptr->toString(k) // k.toString()
            << std::endl;

  // Class l = k.getClass();
  __rt::checkNotNull(k);
  Class l = k->__vptr->getClass(k);

  __rt::checkNotNull(l);
  std::cout << "l.getName()  : "
            << l->__vptr->getName(l) // l.getName()
            << std::endl
            << "l.toString() : "
            << l->__vptr->toString(l) // l.toString()
            << std::endl;

  // if (k.equals(l)) { ... } else { ... }
  __rt::checkNotNull(k);
  if (k->__vptr->equals(k, l)) {
    std::cout << "k.equals(l)" << std::endl;
  } else {
    std::cout << "! k.equals(l)" << std::endl;
  }

  // if (k.equals(l.getSuperclass())) { ... } else { ... }
  __rt::checkNotNull(k);
  __rt::checkNotNull(l);
  if (k->__vptr->equals(k, l->__vptr->getSuperclass(l))) {
    std::cout << "k.equals(l.getSuperclass())" << std::endl;
  } else {
    std::cout << "! k.equals(l.getSuperclass())" << std::endl;
  }

  // if (k.isInstance(o)) { ... } else { ... }
  __rt::checkNotNull(k);
  if (k->__vptr->isInstance(k, o)) {
    std::cout << "o instanceof k" << std::endl;
  } else {
    std::cout << "! (o instanceof k)" << std::endl;
  }

  // if (l.isInstance(o)) { ... } else { ... }
  __rt::checkNotNull(l);
  if (l->__vptr->isInstance(l, o)) {
    std::cout << "o instanceof l" << std::endl;
  } else {
    std::cout << "! (o instanceof l)" << std::endl;
  }

  // HACK: Calling java.lang.Object.toString on k
  std::cout << o->__vptr->toString(k) << std::endl;

  // int[] a = new int[5];
  __rt::Ptr<__rt::Array<int32_t> > a = new __rt::Array<int32_t>(5);

  // a[2]
  __rt::checkNotNull(a);
  std::cout << "a[2]  : " << (*a)[2] << std::endl;

  // a[2] = 5;
  __rt::checkNotNull(a);
  (*a)[2] = 5;

  // a[2]
  __rt::checkNotNull(a);
  std::cout << "a[2]  : " << (*a)[2] << std::endl;

  // String[] ss = new String[5];
  __rt::Ptr<__rt::Array<String> > ss = new __rt::Array<String>(5);

  // String s = "Hello";
  String s = __rt::literal("Hello");

  // ss[2] = "Hello";
  __rt::checkNotNull(ss);
  __rt::checkStore(ss, s);
  (*ss)[2] = s;

  std::cout << "ss[2] : " << (*ss)[2] << std::endl;

  // Upcast.
  o = k;

  // Downcast.
  k = __rt::java_cast<Class>(o);

  // Done.
  std::cout << "--------------------------------------------------------------"
            << "----------------"
            << std::endl;
  return 0;
}
