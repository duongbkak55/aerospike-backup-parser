## Backup File Format

Currently, there is only a single, text-based backup file format, which provides compatibility with previous versions of Aerospike. However, backup file formats are pluggable and a binary format could be supported in the future.

Regardless of the format, any backup file starts with a header line that identifies it as an Aerospike backup file and specifies the version of the backup file format.

    ["Version"] [SP] ["3.1"] [LF]

Let's use the above to agree on a few things regarding notation.

* `["Version"]` is a 7-character string literal that consists of the letters `V`, `e`, `r`, `s`, `i`, `o`, and `n`. Likewise, `["3.1"]` is a 3-character string literal.

* `[SP]` is a single space character (ASCII code 32).

* `[LF]` is a single line feed character (ASCII code 10).

Note that the backup file format is pretty strict. When the specification says `[SP]`, it really means a single space character: not more than one, no tabs, etc. Also, `[LF]` really is a single line feed character: no carriage returns, not more than one (i.e., no empty lines), etc. Maybe it's helpful to look at the text-based format as a binary format that just happens to be readable by humans.

### Meta Data Section

The header line is always followed by zero or more lines that contain meta information about the backup file ("meta data section"). These lines always start with a `["#"] [SP]` prefix. Currently, there are two different meta information lines.

    ["#"] [SP] ["namespace"] [SP] [escape({namespace})] [LF]
    ["#"] [SP] ["first-file"] [LF]

* The first line specifies the namespace from which this backup file was created.

* The second line is optional and marks this backup file as the first in a set of backup files. We discussed above what exactly this means and why it is important.

We also introduced a new notation, `escape(...)`. Technically, a namespace identifier can contain space characters or line feeds. As the backup file format uses spaces and line feeds as token separators, they need to be escaped when they appear inside a token. We escape a token by adding a backslash ("\\") character before any spaces, line feeds, and backslashes in the token. And that's what `escape(...)` means.

Escaping naively works on bytes. It thus works without having to know about character encodings. If we have a UTF-8 string that contains a byte with value 32, this byte will be escaped regardless of whether it is an actual space character or part of the encoding of a Unicode code point.

We also introduced placeholders into the notation, which represent dynamic data. `{namespace}` is a placeholder for, and thus replaced by, the namespace that the backup file belongs to.

For a namespace `Name Space`, the meta data line would look like this.

    ["#"] [SP] ["namespace"] [SP] ["Name\ Space"] [LF]

In general, any byte value is allowed in the dynamic data represented by a placeholder. The exception to that are placeholders that are passed to `escape(...)`.  As a general rule of thumb, escaped data may not contain NUL bytes. This is due to the design of the Aerospike C client API, which, for developer convenience, uses NUL-terminated C strings for things like namespace, set, or index names. (Not for bin values, though. Those may very well contain NUL bytes and, in line with the rule of thumb, aren't escaped. See below.)

### Global Section

The meta data section is always followed by zero or more lines that contain global cluster data, i.e., data that pertains to all nodes in the cluster ("global section"). This data currently encompasses secondary indexes and UDF files.

Lines in the global section always start with a `["*"] [SP]` prefix. Let's first look at lines that describe secondary indexes.

    ["*"] [SP] ["i"] [SP] [escape({namespace})] [SP] [escape({set})] [SP] [escape({name})] [SP]
               [{index-type}] [SP] ["1"] [SP] [escape({path})] [SP] [{data-type}] [LF]

Or if a secondary index has a context defined on it.

    ["*"] [SP] ["i"] [SP] [escape({namespace})] [SP] [escape({set})] [SP] [escape({name})] [SP]
               [{index-type}] [SP] ["1"] [SP] [escape({path})] [SP] [{data-type}] [SP] [{context}] [LF]


Let's look at the placeholders, there are quite a few.

| Placeholder    | Content |
|----------------|---------|
| `{namespace}`  | The namespace that the index applies to. |
| `{set}`        | The set that the index applies to. Note that this can be empty, i.e., a zero-length string, as indexes do not necessarily have to be associated with a set. |
| `{name}`       | The name of the index. |
| `{index-type}` | The type of index: `N` = index on bins, `L` = index on list elements, `K` = index on map keys, `V` = index on map values |
| `{path}`       | The bin name |
| `{data-type}`  | The data type of the indexed value: `N` = numeric, `S` = string, `G` = geo2dsphere, `B` = bytes/blob, `I` = invalid |
| `{context}` | This and the white space character before it are optional. It is the base64 encoded CDT context for secondary index defined on a CDT element.  |

The `["1"]` token is actually the number of values covered by the index. This is for future extensibility, i.e., for composite indexes that span more than one value. Right now, this token is always `["1"]`, though.

Let's now look at how UDF files are represented in the global section.

    ["*"] [SP] ["u"] [SP] [{type}] [SP] [escape({name})] [SP] [{length}] [SP] [{content}] [LF]

Here's what the placeholders stand for.

| Placeholder | Content |
|-------------|---------|
| `{type}`    | The type of the UDF file. Currently always `L` for Lua. |
| `{name}`    | The file name of the UDF file. |
| `{length}`  | The length of the UDF file, which is a decimal unsigned 32-bit value. |
| `{content}` | The content of the UDF file: `{length}` raw bytes of data. The UDF file is simply copied to the backup file. As we know the length of the UDF file, no escaping is required. Also, the UDF file will most likely contain line feeds, so this "line" will actually span multiple lines in the backup file. |

### Records Section

The global section is followed by zero or more records. Each record starts with a multi-line header. Record header lines always start with a `["+"] [SP]` prefix. They have to appear in the given order. Two of these lines are optional, though.

The first line is optional. It is only present, if the actual value of the key -- as opposed to just the key digest -- was stored with the record. If it is present, it has one of the following four forms, depending on whether the key value is an integer, a double, a string, or a bytes value.

    ["+"] [SP] ["k"] [SP] ["I"] [SP] [{int-value}] [LF]
    ["+"] [SP] ["k"] [SP] ["D"] [SP] [{float-value}] [LF]
    ["+"] [SP] ["k"] [SP] ["S"] [SP] [{string-length}] [SP] [{string-data}] [LF]
    ["+"] [SP] ["k"] [SP] ["B"] ["!"]? [SP] [{bytes-length}] [SP] [{bytes-data}] [LF]

Note that we introduced one last notation, `[...]?`, which indicates that a token is optional. For bytes-valued keys, `["B"]` can thus optionally be followed by `["!"]`.

Here's what the placeholders in the above four forms mean.

| Placeholder       | Content |
|-------------------|---------|
| `{int-value}`     | The signed decimal 64-bit integer value of the key. |
| `{float-value}`   | The decimal 64-bit floating point value of the key, including `nan`, `+inf`, and `-inf`. |
| `{string-length}` | The length of the string value of the key, measured in raw bytes; an unsigned decimal 32-bit value. |
| `{string-data}`   | The content of the string value of the key: `{string-length}` raw bytes of data; not escaped, may contain NUL, etc. |
| `{bytes-length}`  | If `["!"]` present: The length of the bytes value of the key.<br>Else: The length of the base-64 encoded bytes value of the key.<br>In any case, an unsigned decimal 32-bit value. |
| `{bytes-data}`    | If `["!"]` present: The content of the bytes value of the key: `{bytes-length}` raw bytes of data; not escaped, may contain NUL, etc.<br>Else: The base-64 encoded content of the bytes value of the key: `{bytes-length}` base-64 characters. |

The next two lines of the record header specify the namespace of the record and its key digest and look like this.

    ["+"] [SP] ["n"] [SP] [escape({namespace})] [LF]
    ["+"] [SP] ["d"] [SP] [{digest}] [LF]

`{namespace}` is the namespace of the record, `{digest}` is its base-64 encoded key digest.

The next line is optional again. It specifies the set of the record.

    ["+"] [SP] ["s"] [SP] [escape({set})] [LF]

`{set}` is the set of the record.

The remainder of the record header specifies the generation count, the expiration time, and the bin count of the record. It looks as follows.

    ["+"] [SP] ["g"] [SP] [{gen-count}] [LF]
    ["+"] [SP] ["t"] [SP] [{expiration}] [LF]
    ["+"] [SP] ["b"] [SP] [{bin-count}] [LF]

Here's what the above placeholders stand for.

| Placeholder    | Content |
|----------------|---------|
| `{gen-count}`  | The record generation count. An unsigned 16-bit decimal integer value. |
| `{expiration}` | The record expiration time in seconds since the Aerospike epoch (2010-01-01 00:00:00 UTC). An unsigned decimal 32-bit integer value. |
| `{bin-count}`  | The number of bins in the record. An unsigned decimal 16-bit integer value. |

The record header lines are followed by `{bin-count}`-many lines of bin data. Each bin data line starts with a `["-"] [SP]` prefix. Depending on the bin data type, a bin data line can generally have one of the following five forms.

    ["-"] [SP] ["N"] [SP] [escape({bin-name})] [LF]
    ["-"] [SP] ["Z"] [SP] [escape({bin-name})] [SP] [{bool-value}] [LF]
    ["-"] [SP] ["I"] [SP] [escape({bin-name})] [SP] [{int-value}] [LF]
    ["-"] [SP] ["D"] [SP] [escape({bin-name})] [SP] [{float-value}] [LF]
    ["-"] [SP] ["S"] [SP] [escape({bin-name})] [SP] [{string-length}] [SP] [{string-data}] [LF]
    ["-"] [SP] ["B"] ["!"]? [SP] [escape({bin-name})] [SP] [{bytes-length}] [SP] [{bytes-data}] [LF]

The first form represents a `NIL`-valued bin. The remaining five forms represent a boolean-valued, an integer-valued, a double-valued, a string-valued, and a bytes-valued bin. Except for the boolean bins, they are completely analogous to the above four forms for an integer, a double, a string, and a bytes record key value. Accordingly, the placeholders `{int-value}`, `{float-value}`, `{string-length}`, `{string-data}`, `{bytes-length}`, and `{bytes-data}` work in exactly the same way -- just for bin values instead of key values.

| Placeholder       | Content |
|-------------------|---------|
| `{bin-name}`      | The name of the bin. |
| `{bool-value}`    | The boolean value of the bin. |
| `{int-value}`     | The signed decimal 64-bit integer value of the bin. |
| `{float-value}`   | The decimal 64-bit floating point value of the bin, including `nan`, `+inf`, and `-inf`. |
| `{string-length}` | The length of the string value of the bin, measured in raw bytes; an unsigned decimal 32-bit value. |
| `{string-data}`   | The content of the string value of the bin: `{string-length}` raw bytes of data; not escaped, may contain NUL, etc. |
| `{bytes-length}`  | If `["!"]` present: The length of the bytes value of the bin.<br>Else: The length of the base-64 encoded bytes value of the bin.<br>In any case, an unsigned decimal 32-bit value. |
| `{bytes-data}`    | If `["!"]` present: The content of the bytes value of the bin: `{bytes-length}` raw bytes of data; not escaped, may contain NUL, etc.<br>Else: The base-64 encoded content of the bytes value of the bin: `{bytes-length}` base-64 characters. |

Actually, the above `["B"]` form is not the only way to represent bytes-valued bins. It gets a little more specific than that. There are other tokens that refer to more specific bytes values. In particular, list-valued and map-valued bins are represented as a bytes value.

| Token   | Type                                                          |
|---------|---------------------------------------------------------------|
| `["B"]` | Generic bytes value.                                          |
| `["J"]` | Java bytes value.                                             |
| `["C"]` | C# bytes value.                                               |
| `["P"]` | Python bytes value.                                           |
| `["R"]` | Ruby bytes value.                                             |
| `["H"]` | PHP bytes value.                                              |
| `["E"]` | Erlang bytes value.                                           |
| `["Y"]` | HyperLogLog, opaquely represented as a bytes value.           |
| `["M"]` | Map value, opaquely represented as a bytes value.             |
| `["L"]` | List value, opaquely represented as a bytes value.            |

### Sample Backup File

The following backup file contains two secondary indexes, a UDF file, and a record. The two empty lines stem from the UDF file, which contains two line feeds.

    Version 3.1
    # namespace test
    # first-file
    * i test test-set int-index N 1 int-bin N
    * i test test-set string-index N 1 string-bin S
    * u L test.lua 27 -- just an empty Lua file


    + n test
    + d q+LsiGs1gD9duJDbzQSXytajtCY=
    + s test-set
    + g 1
    + t 0
    + b 2
    - I int-bin 12345
    - S string-bin 5 abcde

In greater detail:

* The backup was taken from namespace `test` and set `test-set`.

* The record never expires and it has two bins: an integer bin, `int-bin`, and a string bin, `string-bin`, with values `12345` and `"abcde"`, respectively.

* The secondary indexes are `int-index` for the integer bin and `string-index` for the string bin.

* The UDF file is `test.lua` and contains 27 bytes.

Let's also look at the corresponding hex dump for a little more insight regarding the UDF file and its line feeds.

    0000: 5665 7273 696f 6e20 332e 310a 2320 6e61  Version 3.1.# na
    0010: 6d65 7370 6163 6520 7465 7374 0a23 2066  mespace test.# f
    0020: 6972 7374 2d66 696c 650a 2a20 6920 7465  irst-file.* i te
    0030: 7374 2074 6573 742d 7365 7420 696e 742d  st test-set int-
    0040: 696e 6465 7820 4e20 3120 696e 742d 6269  index N 1 int-bi
    0050: 6e20 4e0a 2a20 6920 7465 7374 2074 6573  n N.* i test tes
    0060: 742d 7365 7420 7374 7269 6e67 2d69 6e64  t-set string-ind
    0070: 6578 204e 2031 2073 7472 696e 672d 6269  ex N 1 string-bi
    0080: 6e20 530a 2a20 7520 4c20 7465 7374 2e6c  n S.* u L test.l
    0090: 7561 2032 3720 2d2d 206a 7573 7420 616e  ua 27 -- just an
    00a0: 2065 6d70 7479 204c 7561 2066 696c 650a   empty Lua file.
    00b0: 0a0a 2b20 6e20 7465 7374 0a2b 2064 2071  ..+ n test.+ d q
    00c0: 2b4c 7369 4773 3167 4439 6475 4a44 627a  +LsiGs1gD9duJDbz
    00d0: 5153 5879 7461 6a74 4359 3d0a 2b20 7320  QSXytajtCY=.+ s
    00e0: 7465 7374 2d73 6574 0a2b 2067 2031 0a2b  test-set.+ g 1.+
    00f0: 2074 2030 0a2b 2062 2032 0a2d 2049 2069   t 0.+ b 2.- I i
    0100: 6e74 2d62 696e 2031 3233 3435 0a2d 2053  nt-bin 12345.- S
    0110: 2073 7472 696e 672d 6269 6e20 3520 6162   string-bin 5 ab
    0120: 6364 650a                                cde.

The content of the Lua file consists of the 27 bytes at offsets 0x096 through 0x0b0. The line feed at 0xb0 still belongs to the Lua file, the line feed at 0xb1 is the line feed dictated by the backup file format.
import java.io.*;

public class AerospikeBackupParser {

    public void parseBackupFile(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Version")) {
                    parseHeader(line);
                } else if (line.startsWith("#")) {
                    parseMetaData(line);
                } else if (line.startsWith("*")) {
                    parseGlobalData(line);
                } else if (line.startsWith("+")) {
                    parseRecordHeader(line, reader);
                } else if (line.startsWith("-")) {
                    parseBinData(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parseHeader(String line) {
        // Implement parsing logic for the header line
    }

    private void parseMetaData(String line) {
        // Implement parsing logic for the meta data section
    }

    private void parseGlobalData(String line) {
        // Implement parsing logic for the global section data
    }

    private void parseRecordHeader(String line, BufferedReader reader) throws IOException {
        // Implement parsing logic for the record header
    }

    private void parseBinData(String line) {
        // Implement parsing logic for the bin data line
    }
}