here’s a summary and breakdown of the Aerospike backup file format:

Notation
["Version"] [SP] ["3.1"] [LF]: Indicates the start of the file with the version number.
["Version"]: A 7-character string literal.
["3.1"]: A 3-character string literal.
[SP]: A single space character (ASCII 32).
[LF]: A single line feed character (ASCII 10).
Meta Data Section
Lines in this section start with ["#"] [SP] and provide meta-information:

["#"] [SP] ["namespace"] [SP] [escape({namespace})] [LF]: Specifies the namespace of the backup.
["#"] [SP] ["first-file"] [LF]: (Optional) Marks the file as the first in a set of backup files.
Global Section
Lines in this section start with ["*"] [SP] and contain global cluster data:

Secondary Indexes:
["*"] [SP] ["i"] [SP] [escape({namespace})] [SP] [escape({set})] [SP] [escape({name})] [SP] [{index-type}] [SP] ["1"] [SP] [escape({path})] [SP] [{data-type}] [LF]
With context: ["*"] [SP] ["i"] [SP] [escape({namespace})] [SP] [escape({set})] [SP] [escape({name})] [SP] [{index-type}] [SP] ["1"] [SP] [escape({path})] [SP] [{data-type}] [SP] [{context}] [LF]
UDF Files:
["*"] [SP] ["u"] [SP] [{type}] [SP] [escape({name})] [SP] [{length}] [SP] [{content}] [LF]
Records Section
Each record starts with a multi-line header, with lines starting with ["+"] [SP]:

Key Values:
Integer: ["+"] [SP] ["k"] [SP] ["I"] [SP] [{int-value}] [LF]
Double: ["+"] [SP] ["k"] [SP] ["D"] [SP] [{float-value}] [LF]
String: ["+"] [SP] ["k"] [SP] ["S"] [SP] [{string-length}] [SP] [{string-data}] [LF]
Bytes: ["+"] [SP] ["k"] [SP] ["B"] ["!"]? [SP] [{bytes-length}] [SP] [{bytes-data}] [LF]
Namespace and Key Digest:
["+"] [SP] ["n"] [SP] [escape({namespace})] [LF]
["+"] [SP] ["d"] [SP] [{digest}] [LF]
(Optional) Set:
["+"] [SP] ["s"] [SP] [escape({set})] [LF]
Generation, Expiration, Bin Count:
["+"] [SP] ["g"] [SP] [{gen-count}] [LF]
["+"] [SP] ["t"] [SP] [{expiration}] [LF]
["+"] [SP] ["b"] [SP] [{bin-count}] [LF]
Bin Data
Each bin data line starts with ["-"] [SP] and can have various forms depending on the data type:

NIL: ["-"] [SP] ["N"] [SP] [escape({bin-name})] [LF]
Boolean: ["-"] [SP] ["Z"] [SP] [escape({bin-name})] [SP] [{bool-value}] [LF]
Integer: ["-"] [SP] ["I"] [SP] [escape({bin-name})] [SP] [{int-value}] [LF]
Float: ["-"] [SP] ["D"] [SP] [escape({bin-name})] [SP] [{float-value}] [LF]
String: ["-"] [SP] ["S"] [SP] [escape({bin-name})] [SP] [{string-length}] [SP] [{string-data}] [LF]
Bytes: ["-"] [SP] ["B"] ["!"]? [SP] [escape({bin-name})] [SP] [{bytes-length}] [SP] [{bytes-data}] [LF]
Specific Bytes Types
Generic: ["B"]
Java: ["J"]
C#: `["C
