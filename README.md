# Version 1.0.0
### Project
- [x] test trigger module of dump_julia
- [x] Julia Parser
- [x] Type Interferences
- [x] Html Output

### Syntactic features
- [x] Chained Arithmetic
- [ ] Auto Return Last Expr
 
### Metaprogramming
- [ ] macro support for julia
- [ ] Quote & Expr ... type

### Parallelism
- [ ] Vectorized "dot" Operators

### Miscs
- [ ] Build-in Library Analysis
- [ ] assign in global expr (waiting for CSTParser fix)

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