package org.atnos.eff

import cats.data.Xor
import scala.annotation.implicitNotFound

@implicitNotFound("No instance found for MemberIn[${T}, ${R}].\nThe effect ${T} is not part of the stack ${R}")
trait MemberIn[T[_], R] {
  def inject[V](tv: T[V]): Union[R, V]
}

object MemberIn extends MemberInLower1 {

  @implicitNotFound("No instance found for MemberIn[${T}, ${R}].\nThe effect ${T} is not part of the stack ${R}")
  type |=[T[_], R] = MemberIn[T, R]
}

trait MemberInLower1 extends MemberInLower2 {
  implicit def MemberIn1[T[_]]: MemberIn[T, Fx1[T]] = new MemberIn[T, Fx1[T]] {
    def inject[V](effect: T[V]): Union[Fx1[T], V] =
      Union1(effect)
  }
}

trait MemberInLower2 extends MemberInLower3 {
  implicit def MemberIn2L[L[_], R[_]]: MemberIn[L, Fx2[L, R]] = new MemberIn[L, Fx2[L, R]] {
    def inject[V](effect: L[V]): Union[Fx2[L, R], V] =
      Union2L(effect)
  }

  implicit def MemberIn3L[L[_], M[_], R[_]]: MemberIn[L, Fx3[L, M, R]] = new MemberIn[L, Fx3[L, M, R]] {
    def inject[V](effect: L[V]): Union[Fx3[L, M, R], V] =
      Union3L(effect)
  }

  implicit def MemberInAppendL[T[_], L, R](implicit append: MemberIn[T, L]): MemberIn[T, FxAppend[L, R]] = new MemberIn[T, FxAppend[L, R]] {
    def inject[V](effect: T[V]): Union[FxAppend[L, R], V] =
      UnionAppendL(append.inject(effect))
  }
}

trait MemberInLower3 extends MemberInLower4 {
  implicit def MemberIn2R[L[_], R[_]]: MemberIn[R, Fx2[L, R]] = new MemberIn[R, Fx2[L, R]] {
    def inject[V](effect: R[V]): Union[Fx2[L, R], V] =
      Union2R(effect)
  }

  implicit def MemberIn3M[L[_], M[_], R[_]]: MemberIn[M, Fx3[L, M, R]] = new MemberIn[M, Fx3[L, M, R]] {
    def inject[V](effect: M[V]): Union[Fx3[L, M, R], V] =
      Union3M(effect)
  }

  implicit def MemberInAppendR[T[_], L, R](implicit append: MemberIn[T, R]): MemberIn[T, FxAppend[L, R]] = new MemberIn[T, FxAppend[L, R]] {
    def inject[V](effect: T[V]): Union[FxAppend[L, R], V] =
      UnionAppendR(append.inject(effect))
  }
}

trait MemberInLower4 extends MemberInLower5 {
  implicit def MemberIn3R[L[_], M[_], R[_]]: MemberIn[R, Fx3[L, M, R]] = new MemberIn[R, Fx3[L, M, R]] {
    def inject[V](effect: R[V]): Union[Fx3[L, M, R], V] =
      Union3R(effect)
  }
}

trait MemberInLower5 {
  implicit def MemberInAppendAnyL[T[_], R]: MemberIn[T, FxAppend[Fx1[T], R]] = new MemberIn[T, FxAppend[Fx1[T], R]] {
    def inject[V](effect: T[V]): Union[FxAppend[Fx1[T], R], V] =
      UnionAppendL(Union1(effect))
  }

  implicit def MemberInAppendAnyR[T[_], L, R](implicit m: MemberIn[T, R]): MemberIn[T, FxAppend[L, R]] = new MemberIn[T, FxAppend[L, R]] {
    def inject[V](effect: T[V]): Union[FxAppend[L, R], V] =
      UnionAppendR(m.inject(effect))
  }
}

@implicitNotFound("No instance found for Member[${T}, ${R}].\nThe effect ${T} is not part of the stack ${R}\n or it was not possible to determine the stack that would result from removing ${T} from ${R}")
trait Member[T[_], R] extends MemberIn[T, R] {
  type Out

  def accept[V](union: Union[Out, V]): Union[R, V]

  def project[V](union: Union[R, V]): Union[Out, V] Xor T[V]

  def aux: Member.Aux[T, R, Out] =
    this
}

object Member extends MemberLower1 {

  @implicitNotFound("No instance found for Member[${T}, ${R}].\nThe effect ${T} is not part of the stack ${R}\n or it was not possible to determine the stack that would result from removing ${T} from ${R}")
  type <=[T[_], R] = Member[T, R]

  @implicitNotFound("No instance found for Member[${T}, ${R}].\nThe effect ${T} is not part of the stack ${R}\n or it was not possible to determine the stack that would result from removing ${T} from ${R}")
  type Aux[T[_], R, U] = Member[T, R] { type Out = U }

  def apply[T[_], R](implicit m: Member[T, R]): Member[T, R] =
    m

  def aux[T[_], R, U](implicit m: Member.Aux[T, R, U]): Member.Aux[T, R, U] =
    m

  def unaux[T[_], R, U](implicit m: Member.Aux[T, R, U]): Member[T, R] =
    m
}

// 1.  Member[T, Fx1[T]]
// 2.  Member[L, Fx2[L, R]]
// 3.  Member[L, Fx3[L, M, R]]
// 4.  Member[T, FxAppend[Fx1[T], Fx3[L, M, R]]]
// 5.  Member[L, FxAppend[Fx1[T], Fx3[L, M, R]]]
// 6.  Member[M, FxAppend[Fx1[T], Fx3[L, M, R]]]
// 7.  Member[R, FxAppend[Fx1[T], Fx3[L, M, R]]]
// 8.  Member[T, FxAppend[Fx1[T], R]]
// 9.  Member[R, Fx2[L, R]]
// 10. Member[T, FxAppend[L, R]]
// 11. Member[M, Fx3[L, M, R]]
// 12. Member[T, FxAppend[L, R]]
// 13. Member[R, Fx3[L, M, R]]



trait MemberLower1 extends MemberLower2 {

  implicit def Member1[T[_]]: Member.Aux[T, Fx1[T], NoFx] = new Member[T, Fx1[T]] { outer =>
    type Out = NoFx

    def inject[V](tv: T[V]): Union[Fx1[T], V] =
      Union1[T, V](tv)

    def accept[V](union: Union[Out, V]): Union[Fx1[T], V] =
      sys.error("cannot accept a nil effect as In1")

    def project[V](union: Union[Fx1[T], V]): Union[Out, V] Xor T[V] =
      union match {
        case Union1(e) => Xor.Right(e)
      }
  }

}

trait MemberLower2 extends MemberLower3 {
  implicit def Member2L[L[_], R[_]]: Member.Aux[L, Fx2[L, R], Fx1[R]] = new Member[L, Fx2[L, R]] { outer =>
    type Out = Fx1[R]

    def inject[V](tv: L[V]): Union[Fx2[L, R], V] =
      Union2L[L, R, V](tv)

    def accept[V](union: Union[Out, V]): Union[Fx2[L, R], V] =
      union match {
        case Union1(e) => Union2R(e)
      }

    def project[V](union: Union[Fx2[L, R], V]): Union[Out, V] Xor L[V] =
      union match {
        case Union2L(e) => Xor.Right(e)
        case Union2R(e) => Xor.Left(Union1[R, V](e))
      }
  }
}

trait MemberLower3 extends MemberLower4 {

  implicit def Member3L[L[_], M[_], R[_]]: Member.Aux[L, Fx3[L, M, R], Fx2[M, R]] = new Member[L, Fx3[L, M, R]] { outer =>
    type Out = Fx2[M, R]

    def inject[V](tv: L[V]): Union[Fx3[L, M, R], V] =
      Union3L[L, M, R, V](tv)

    def accept[V](union: Union[Out, V]): Union[Fx3[L, M, R], V] =
      union match {
        case Union2L(e) => Union3M(e)
        case Union2R(e) => Union3R(e)
      }

    def project[V](union: Union[Fx3[L, M, R], V]): Union[Out, V] Xor L[V] =
      union match {
        case Union3L(e) => Xor.Right(e)
        case Union3M(e) => Xor.Left(Union2L[M, R, V](e))
        case Union3R(e) => Xor.Left(Union2R[M, R, V](e))
      }
  }

}

trait MemberLower4 extends MemberLower5 {

  implicit def Member4L[T[_], L[_], M[_], R[_]]: Member.Aux[T, FxAppend[Fx1[T], Fx3[L, M, R]], Fx3[L, M, R]] = new Member[T, FxAppend[Fx1[T], Fx3[L, M, R]]] { outer =>
    type Out = Fx3[L, M, R]

    def inject[V](tv: T[V]): Union[FxAppend[Fx1[T], Out], V] =
      UnionAppendL[Fx1[T], Fx3[L, M, R], V](Union1(tv))

    def accept[V](union: Union[Out, V]): Union[FxAppend[Fx1[T], Fx3[L, M, R]], V] =
      UnionAppendR(union)

    def project[V](union: Union[FxAppend[Fx1[T], Fx3[L, M, R]], V]): Union[Out, V] Xor T[V] =
      union match {
        case UnionAppendL(Union1(e)) => Xor.Right(e)
        case UnionAppendR(u)         => Xor.left(u)
      }
  }

}

trait MemberLower5 extends MemberLower6 {
  implicit def Member4RL[T[_], L[_], M[_], R[_]]: Member.Aux[L, FxAppend[Fx1[T], Fx3[L, M, R]], Fx3[T, M, R]] = new Member[L, FxAppend[Fx1[T], Fx3[L, M, R]]] { outer =>
    type Out = Fx3[T, M, R]

    def inject[V](l: L[V]): Union[FxAppend[Fx1[T], Fx3[L, M, R]], V] =
      UnionAppendR[Fx1[T], Fx3[L, M, R], V](Union3L(l))

    def accept[V](union: Union[Fx3[T, M, R], V]): Union[FxAppend[Fx1[T], Fx3[L, M, R]], V] =
      union match {
        case Union3L(t) => UnionAppendL(Union1(t))
        case Union3M(m) => UnionAppendR(Union3M(m))
        case Union3R(r) => UnionAppendR(Union3R(r))
      }

    def project[V](union: Union[FxAppend[Fx1[T], Fx3[L, M, R]], V]): Union[Fx3[T, M, R], V] Xor L[V] =
      union match {
        case UnionAppendL(Union1(t))  => Xor.left(Union3L(t))
        case UnionAppendR(Union3L(l)) => Xor.Right(l)
        case UnionAppendR(Union3M(m)) => Xor.Left(Union3M(m))
        case UnionAppendR(Union3R(r)) => Xor.Left(Union3R(r))
        case UnionAppendR(_)          => sys.error("appeasing the compiler exhaustiveness checking for Member4RL")
      }
  }
}

trait MemberLower6 extends MemberLower7 {

  implicit def Member4RM[T[_], L[_], M[_], R[_]]: Member.Aux[M, FxAppend[Fx1[T], Fx3[L, M, R]], Fx3[T, L, R]] = new Member[M, FxAppend[Fx1[T], Fx3[L, M, R]]] { outer =>
    type Out = Fx3[T, L, R]

    def inject[V](m: M[V]): Union[FxAppend[Fx1[T], Fx3[L, M, R]], V] =
      UnionAppendR[Fx1[T], Fx3[L, M, R], V](Union3M(m))

    def accept[V](union: Union[Fx3[T, L, R], V]): Union[FxAppend[Fx1[T], Fx3[L, M, R]], V] =
      union match {
        case Union3L(t) => UnionAppendL(Union1(t))
        case Union3M(l) => UnionAppendR(Union3L(l))
        case Union3R(r) => UnionAppendR(Union3R(r))
      }

    def project[V](union: Union[FxAppend[Fx1[T], Fx3[L, M, R]], V]): Union[Fx3[T, L, R], V] Xor M[V] =
      union match {
        case UnionAppendL(Union1(t))  => Xor.left(Union3L(t))
        case UnionAppendR(Union3L(l)) => Xor.left(Union3M(l))
        case UnionAppendR(Union3M(m)) => Xor.Right(m)
        case UnionAppendR(Union3R(r)) => Xor.Left(Union3R(r))
        case UnionAppendR(_)          => sys.error("appeasing the compiler exhaustiveness checking for Member4RM")
      }
  }

}

trait MemberLower7 extends MemberLower8 {

  implicit def Member4RR[T[_], L[_], M[_], R[_]]: Member.Aux[R, FxAppend[Fx1[T], Fx3[L, M, R]], Fx3[T, L, M]] = new Member[R, FxAppend[Fx1[T], Fx3[L, M, R]]] { outer =>
    type Out = Fx3[T, L, M]

    def inject[V](r: R[V]): Union[FxAppend[Fx1[T], Fx3[L, M, R]], V] =
      UnionAppendR[Fx1[T], Fx3[L, M, R], V](Union3R(r))

    def accept[V](union: Union[Fx3[T, L, M], V]): Union[FxAppend[Fx1[T], Fx3[L, M, R]], V] =
      union match {
        case Union3L(t) => UnionAppendL(Union1(t))
        case Union3M(l) => UnionAppendR(Union3L(l))
        case Union3R(m) => UnionAppendR(Union3M(m))
      }

    def project[V](union: Union[FxAppend[Fx1[T], Fx3[L, M, R]], V]): Union[Fx3[T, L, M], V] Xor R[V] =
      union match {
        case UnionAppendL(Union1(t))  => Xor.left(Union3L(t))
        case UnionAppendR(Union3L(l)) => Xor.left(Union3M(l))
        case UnionAppendR(Union3M(m)) => Xor.left(Union3R(m))
        case UnionAppendR(Union3R(r)) => Xor.Right(r)
        case UnionAppendR(_)          => sys.error("appeasing the compiler exhaustiveness checking for Member4RM")
      }
  }
}

trait MemberLower8 extends MemberLower9 {
  implicit def MemberAppend1L[T[_], R]: Member.Aux[T, FxAppend[Fx1[T], R], R] = new Member[T, FxAppend[Fx1[T], R]] {
    type Out = R

    def inject[V](e: T[V]): Union[FxAppend[Fx1[T], R], V] =
      UnionAppendL(Union1(e))

    def accept[V](union: Union[Out, V]): Union[FxAppend[Fx1[T], R], V] =
      UnionAppendR(union)

    def project[V](union: Union[FxAppend[Fx1[T], R], V]): Union[Out, V] Xor T[V] =
      union match {
        case UnionAppendL(Union1(e)) => Xor.Right(e)
        case UnionAppendR(u)         => Xor.Left(u)
      }
  }
}

trait MemberLower9 extends MemberLower10 {

  implicit def Member2R[L[_], R[_]]: Member.Aux[R, Fx2[L, R], Fx1[L]] = new Member[R, Fx2[L, R]] { outer =>
    type Out = Fx1[L]

    def inject[V](tv: R[V]): Union[Fx2[L, R], V] =
      Union2R[L, R, V](tv)

    def accept[V](union: Union[Out, V]): Union[Fx2[L, R], V] =
      union match {
        case Union1(e) => Union2L(e)
      }

    def project[V](union: Union[Fx2[L, R], V]): Union[Out, V] Xor R[V] =
      union match {
        case Union2R(e) => Xor.Right(e)
        case Union2L(e) => Xor.Left(Union1[L, V](e))
      }

  }
}


trait MemberLower10 extends MemberLower11 {

  implicit def MemberAppendL[T[_], L, R, U](implicit append: Member.Aux[T, L, U]): Member.Aux[T, FxAppend[L, R], FxAppend[U, R]] = new Member[T, FxAppend[L, R]] {
    type Out = FxAppend[U, R]

    def inject[V](effect: T[V]): Union[FxAppend[L, R], V] =
      UnionAppendL(append.inject(effect))

    def accept[V](union: Union[Out, V]): Union[FxAppend[L, R], V] =
      union match {
        case UnionAppendL(u) => UnionAppendL(append.accept(u))
        case UnionAppendR(u) => UnionAppendR(u)
      }

    def project[V](union: Union[FxAppend[L, R], V]): Union[Out, V] Xor T[V] =
      union match {
        case UnionAppendL(u) => append.project(u).leftMap(UnionAppendL.apply)
        case UnionAppendR(u) => Xor.Left(UnionAppendR(u))
      }
  }
}

trait MemberLower11 extends MemberLower12 {

  implicit def Member3M[L[_], M[_], R[_]]: Member.Aux[M, Fx3[L, M, R], Fx2[L, R]] = new Member[M, Fx3[L, M, R]] { outer =>
    type Out = Fx2[L, R]

    def inject[V](tv: M[V]): Union[Fx3[L, M, R], V] =
      Union3M[L, M, R, V](tv)

    def accept[V](union: Union[Out, V]): Union[Fx3[L, M, R], V] =
      union match {
        case Union2L(e) => Union3L(e)
        case Union2R(e) => Union3R(e)
      }

    def project[V](union: Union[Fx3[L, M, R], V]): Union[Out, V] Xor M[V] =
      union match {
        case Union3L(e) => Xor.Left(Union2L[L, R, V](e))
        case Union3M(e) => Xor.Right(e)
        case Union3R(e) => Xor.Left(Union2R[L, R, V](e))
      }
  }

}

trait MemberLower12 extends MemberLower13 {
  implicit def MemberAppendR[T[_], L, R, U](implicit append: Member.Aux[T, R, U]): Member.Aux[T, FxAppend[L, R], FxAppend[L, U]] = new Member[T, FxAppend[L, R]] {
    type Out = FxAppend[L, U]

    def inject[V](effect: T[V]): Union[FxAppend[L, R], V] =
      UnionAppendR(append.inject(effect))

    def accept[V](union: Union[Out, V]): Union[FxAppend[L, R], V] =
      union match {
        case UnionAppendL(u) => UnionAppendL(u)
        case UnionAppendR(u) => UnionAppendR(append.accept(u))
      }

    def project[V](union: Union[FxAppend[L, R], V]): Union[Out, V] Xor T[V] =
      union match {
        case UnionAppendL(u) => Xor.Left(UnionAppendL(u))
        case UnionAppendR(u) => append.project(u).leftMap(UnionAppendR.apply)
      }

  }
}

trait MemberLower13 {

  implicit def Member3R[L[_], M[_], R[_]]: Member.Aux[R, Fx3[L, M, R], Fx2[L, M]] = new Member[R, Fx3[L, M, R]] { outer =>
    type Out = Fx2[L, M]

    def inject[V](tv: R[V]): Union[Fx3[L, M, R], V] =
      Union3R[L, M, R, V](tv)

    def accept[V](union: Union[Out, V]): Union[Fx3[L, M, R], V] =
      union match {
        case Union2L(e) => Union3L(e)
        case Union2R(e) => Union3M(e)
      }

    def project[V](union: Union[Fx3[L, M, R], V]): Union[Out, V] Xor R[V] =
      union match {
        case Union3L(e) => Xor.Left(Union2L[L, M, V](e))
        case Union3M(e) => Xor.Left(Union2R[L, M, V](e))
        case Union3R(e) => Xor.Right(e)
      }
    }
}
