package cyua.java.shared;

// XXX Supports both INT and ENUM bit index
public class BitState
{
long value;

public BitState()
{
}
public BitState(long _val)
{
	setValue(_val);
}

public void setValue(long _val) {value = _val;}
public long getValue() {return value;}
public boolean isZero() {return value == 0;}

@SuppressWarnings("rawtypes")
public void setOnly(Enum ...position)
{
	value = 0;
	for (Enum pos : position) value |= bitVal(pos);
}
public void setOnly(int ...position)
{
	value = 0;
	for (int pos : position) value |= bitVal(pos);
}

public void set(Enum pos, boolean yes)
{
	if (yes) value |= bitVal(pos);
	else value = ~bitVal(pos)&value;
}
public void set(int pos, boolean yes)
{
	if (yes) value |= bitVal(pos);
	else value = ~bitVal(pos)&value;
}
@SuppressWarnings("rawtypes")
public void set(Enum... position)
{
	for (Enum pos : position) value |= bitVal(pos);
}
public void set(int... position)
{
	for (int pos : position) value |= bitVal(pos);
}

@SuppressWarnings("rawtypes")
public void clear(Enum... position)
{
	for (Enum pos : position) value = ~bitVal(pos)&value;
}
public void clear(int... position)
{
	for (int pos : position) value = ~bitVal(pos)&value;
}
public void clear() {value = 0;}

@SuppressWarnings("rawtypes")
public void toggle(Enum... position)
{
	for (Enum pos : position) value ^= bitVal(pos);
}
public void toggle(int... position)
{
	for (int pos : position) value ^= bitVal(pos);
}

@SuppressWarnings("rawtypes")
public boolean has(Enum... position)
{
	for (Enum pos : position)
		if ((value&bitVal(pos)) != 0) return true;
	return false;
}
public boolean has(int... position)
{
	for (int pos : position)
		if ((value&bitVal(pos)) != 0) return true;
	return false;
}

@SuppressWarnings("rawtypes")
public boolean hasAll(Enum ...position)
{
	for (Enum pos : position)
		if ((value&bitVal(pos)) == 0) return false;
	return true;
}
public boolean hasAll(int ...position)
{
	for (int pos : position)
		if ((value&bitVal(pos)) == 0) return false;
	return true;
}

@SuppressWarnings("rawtypes")
public boolean hasOnly(Enum ...position)
{
	int _val = 0;
	for (Enum pos : position) _val |= bitVal(pos);
	return value == _val;
}
public boolean hasOnly(int ...position)
{
	int _val = 0;
	for (int pos : position) _val |= bitVal(pos);
	return value == _val;
}

@Override public String toString()
{
//	return Long.toBinaryString(value);
	StringBuilder text = new StringBuilder();
	float res = value/2f; int $ = 0;
	do
	{
		res = res/2f;
		text.append(text.length() > 0 ? " " : "")
		.append(has($++)?"1":"0");
	}
	while (res >= 0.5);
	return text.toString();
}
public String toString(Enum[] values) {
	StringBuilder text = new StringBuilder();
	for (Enum v : values) {
		if (has(v)) text.append(text.length() > 0 ? ", " : "").append(v.name());
	}
	return text.toString();
}

/*******************************************/
@SuppressWarnings("rawtypes")
private long bitVal(Enum pos) {return (long) Math.pow(2, pos.ordinal());}

private long bitVal(int pos) {return (long) Math.pow(2, pos);}

}

/*
OR ( | ) set flag
0101  0101
0010  0001
0111  0101

AND ( & ) check flag
0101  0101
0010  0100
0000  0100

XOR ( ^ ) toggle flag
0101  0101
0001  0011
0100  0110

NOT ( ~ ) unset (inverts) flag
~0101 > 1010

NOT AND (~N&M)  clear flag
  0101 > 0101
~0100    1011
             0001
*/