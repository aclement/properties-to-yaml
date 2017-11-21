# properties-to-yaml


```
PropertiesToYamlConverter converter = new PropertiesToYamlConverter();
YamlConversionResult result = converter.convert(
  "a.b.c=1\n"+
  "a.b.d=2\n"
);
if (result.getSeverity() != ConversionStatus.OK) {
  throw new IllegalStateException("Problem during conversion: \n"+result.getStatus().getEntries());
}
System.out.println(result.getYaml());
String expected =
  "a:\n"+
  "  b:\n"+
  "    c: '1'\n"+
  "    d: '2'\n";
  if (!result.getYaml().equals(expected)) {
    throw new IllegalStateException("Not expected result! "+result.getYaml());
  }
```
