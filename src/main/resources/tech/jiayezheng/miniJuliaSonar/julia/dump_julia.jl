using CSTParser
using CSTParser: EXPR
using JSON

# 圣诞树🎄
mutable struct DecoratedAST
    julia_sonar_node_type::Any
    value::Any
    has_args::Bool
    args::Array{DecoratedAST}
    # WARNING: May not accurate for recursive expressions
    _start::Int
    _end::Int
    nonlocal::Bool
end


macro TRIVIAL()
    -1
end

filename_processing = "undefined"
const offsetBegin = length("begin ")



function isArithmetic(cst::CSTParser.EXPR)::Bool
    args = cst.args
    if args == nothing
        false
    else
        for arg in args
            if arg.head in (:OPERATOR , :comparison)
                return true
            end
        end
    end
    false
end

function isArithmetic(dst::DecoratedAST)::Bool
    args = dst.args
    if args == nothing
        false
    else
        for arg in args
            if arg.julia_sonar_node_type in (:OPERATOR, :comparison)
                return true
            end
        end
    end
    false
end

hasNoBody(cst) = (cst.args == nothing || cst.args == [])

function build_dst(cst)
    global filename_processing
    function pre_process_no_body(cst)
        head, val = cst.head , cst.val
        # workaorund on function composition sugarelseif head.val == "∘"
#         if cst.val == "∘"
#             head = :FuncCombine
#         end

        return head, val
    end

    function pre_process_with_body(cst)
      # workaorund on assignment
        head = cst.head

        if isa(head,EXPR)
            if head.val == "="
                head = :Assign

            elseif (head.val == "+=" ||
                head.val == "-=" ||
                head.val == "*=" ||
                head.val == "/=")

                head = :AugAssign
            elseif head.val == "::"
                head = :TypeDecl
            elseif head.val == "&&"
                head = :Arithmetic
            elseif head.val == "->"
                head = :Lambda
            elseif head.val == "..."
                head = :VarArg
            elseif head.val == "."
                head = :Dot
            elseif head.val == "<:" || head.val == ">:"
                head = :comparison

            else #
                error("[ERROR] exception detected, please improve dump_julia. cst=",
                        cst, "val=",
                        head.val,
                        " file:", filename_processing)
            end
        end

        return head
    end

    function post_process_with_body(dst)

        function markCall(dst)
            # mark function call or call combination
            args = dst.args
            if args[1].julia_sonar_node_type == :IDENTIFIER
                pushfirst!(args, DecoratedAST(:SCALL,"scall",false,[],@TRIVIAL,@TRIVIAL,false))
            elseif args[1].julia_sonar_node_type == :block
                pushfirst!(args, DecoratedAST(:CCALL,"ccall",false,[],@TRIVIAL,@TRIVIAL,false))
            end
        end

        head = dst.julia_sonar_node_type
        if head == :call
            if isArithmetic(dst)
                head = :Arithmetic
            else #
                markCall(dst)
            end
        elseif head == :brackets
            if isArithmetic(dst)
                head = :Arithmetic
            else
                head = :block
            end
        elseif head == :curly
            head = :ParamType
        end

        dst.julia_sonar_node_type = head
    end

    function post_process_no_body(dst)
        head = dst.julia_sonar_node_type
        value = dst.value
        if head == :IDENTIFIER
            if value == "im"
                head = :IMAGINARY
            elseif value == "missing"
                head = :MISSING
            elseif value == "nothing"
                head = :NOTHING
            end
        end

        dst.julia_sonar_node_type = head
        dst.value = value
    end

    banned(dst) = dst.julia_sonar_node_type == :TRUE && dst.value == nothing

    function build_dst_recursive(cst, offset)
        if hasNoBody(cst)
            head, val = pre_process_no_body(cst)
            # println(offset, ":", offset + cst.span, "\t", CSTParser.valof(cst))
            dst = DecoratedAST(head, val, false , [], offset, offset + cst.span,false)
            offset += cst.fullspan
            #println("\t"* string(dst.value))

            post_process_no_body(dst)
            return dst, offset
        else
            head = pre_process_with_body(cst)

            dst = DecoratedAST(head, cst.val, true,[], @TRIVIAL, @TRIVIAL,false)
            parent = dst
            for a in cst
                child, offset = build_dst_recursive(a, offset)
                !banned(child) && push!(dst.args, child)
            end
            dst._start = dst.args[1]._start
            dst._end = dst.args[1]._end

            post_process_with_body(dst)

            #println(dst.value)
            return dst, offset
        end
    end

    root = DecoratedAST(:ROOT, "ROOT", true, [], @TRIVIAL, @TRIVIAL,false)
    offset = 1 - offsetBegin
    for a in cst
        child, offset = build_dst_recursive(a, offset)
        push!(root.args,child)
    end

    for arg in root.args
        arg.nonlocal = true
    end

    # delete useless begin & end
    popfirst!(root.args)
    pop!(root.args)

    return root
end

# ==================================================
# ||                 UNITILITIES                  ||
# ==================================================
function fix_string_span_unicode!(e::EXPR)
    val = CSTParser.valof(e)
    if typeof(val) == String
        # Hacky length fix
        diff = sizeof(val) - length(val)
        e.span -= diff
        e.fullspan -= diff
    end
end

# print function for experiment
function print_cst(cst)
    function print_cst_recursive(cst, offset)
        if cst.args === nothing
            println(offset, ":", offset + cst.span, "\t", CSTParser.valof(cst))
            offset += cst.fullspan
        else
            for a in cst
                offset = print_cst_recursive(a, offset)
            end
        end

        return offset
    end

    print_cst_recursive(cst, 1)
end


newline(io, level) = write(io,"\r" * repeat("\r",level))

# Override json function
function json(dst::DecoratedAST)
    io = IOBuffer()

    function walk(dst::DecoratedAST, level::Int)
        write(io, "{")

        write(io, "\n"*repeat("\t", level + 1))

        field_len = length(fieldnames(DecoratedAST))

        i = 0
        for fieldname in fieldnames(DecoratedAST)
            field = getfield(dst, fieldname)
            type = fieldtype(DecoratedAST, fieldname)

            # ================================================
            if type == Array{DecoratedAST}
                write(io,"\"")
                write(io,fieldname)
                write(io,"\"")
                write(io,":")
                write(io,"[")

                size = length(field)
                j = 0
                for e in field
                    walk(e, level + 1)
                    if j < size - 1
                        write(io,",\n")
                    end
                    j = j + 1

                end


                write(io,"]")

            elseif type == Bool || type == Int
                write(io,"\"")
                write(io, fieldname)
                write(io,"\"")
                write(io,":")
                write(io,string(field))

            else
                write(io,"\"")
                write(io, fieldname)
                write(io,"\"")
                write(io,":")
                write(io,"\"")
                # handle newline escape
                field_str = string(field)
                for c in field_str
                    if c == '\n'
                        write(io,"\\n")
                    else #
                        write(io,c)
                    end
                end
                write(io,"\"")
            end

            if i < field_len - 1
                write(io,",")
            end
            i = i + 1
            # =====================================================
        end

        write(io, "}")
    end

    walk(dst, 0)

    ret = String(take!(io))
    # return SubString(ret,1,length(ret) - 1)
    return ret
end



# ==================================================
# ||                 ENTRYPOINT                   ||
# ==================================================
function dump_json(filename, output , endmark)
    global filename_processing = filename
    try

        src = open(io -> read(io, String), filename)
        # println(src)

        Meta.parse("begin $src end")
        cst = CSTParser.parse("begin $src end")
        print_cst(cst)

        dst = build_dst(cst)
        out = open(output, "w")

        write(output, json(dst))

        println("$output written !!!")
        close(out)
    finally
        _end = open(endmark, "w")
        close(_end)
    end
end

println("julia dumper started!!!")
# local test code
function test_dump()
    endmark = "end"
    tests = ["test1", "test_macro", "test_short", "test_func"]
    for test in tests
        dump_json(test*".jl", test*".json", endmark)
    end
    rm("./"*endmark)
end

@time test_dump()
println("julia dumper end!!!")