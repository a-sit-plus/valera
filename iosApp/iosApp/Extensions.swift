import shared

public typealias KmpCryptoDigest = Digest

extension Data {
    public var kotlinByteArray : KotlinByteArray {
        let bytes = self.bytes
        let kotlinByteArray = KotlinByteArray(size: Int32(self.count))
        for index in 0..<bytes.count {
            kotlinByteArray.set(index: Int32(index), value: bytes[index])
        }
        return kotlinByteArray
    }

    var bytes: [Int8] {
        return self.map { Int8(bitPattern: $0)}
    }
}

extension Int8 {
    var kotlinByte : KotlinByte {
        return KotlinByte(value: self)
    }
}

extension KotlinByteArray {
    public var data : Data {
        var bytes = [UInt8]()
        for index in 0..<self.size {
            bytes.append(UInt8(bitPattern: self.get(index: index)))
        }
        return Data(bytes)
    }
}


func KmmResultFailure<T>(_ error: KotlinThrowable) -> KmmresultKmmResult<T> where T: AnyObject {
    return KmmresultKmmResult(failure: error) as! KmmresultKmmResult<T>
}

func KmmResultSuccess<T>(_ value: T) -> KmmresultKmmResult<T> where T: AnyObject {
    return KmmresultKmmResult(value: value) as! KmmresultKmmResult<T>
}

