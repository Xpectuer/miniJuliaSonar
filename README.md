# Version 1.0.0
### Overview 

Type inference is a fairly widely used technique, commonly found in IDEs, modern compilers, code bug checking, and even data cleaning. This project implements a type inference framework for Julia, a programming language widely used in practice in the fields of numerical computation and optimization, which models all the static semantics of the [Julia](https://julialang.org/) programming language and provides type inference functionality.

### Project Modules
- [x] test trigger module of dump_julia
- [x] Julia Parser
- [x] Type Interferences
- [x] Html Output

### Syntactic features
- [x] Chained Arithmetic
- [x] Auto Return Last Expr
 
### Metaprogramming
- [x] macro support for julia
- [x] Quote & Expr ... type

### Parallelism
- [x] Vectorized "dot" Operators

### Miscs
- [x] Built-in Library Analysis
- [x] assign in global expr (waiting for CSTParser fix)

### Quick Starts
To run:
```bash
    java -jar target/miniJuliaSonar-<version>.jar <workdir> <outdir>
```

To run GUI:
> Warning: Do not move gui directory. Otherwise, it will not work.
```
    cd gui && npm start
```
