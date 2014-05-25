package de.twimbee.pcml2java;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;

public class PCML2Java {

    private static final String GENERATED_SOURCES_DIR = "target/generated-sources";

    public void createJavaClassesForPCMLFiles(String packageName, String sourceDirectory) {
        try {
            // TODO outputDir configurable? maven restrictions?
            File destDir = new File(GENERATED_SOURCES_DIR);
            if (!destDir.exists()) {
                destDir.mkdirs();
            }
            // delete all files in destination folder
            File destDeleteDir = new File(destDir, packageName.replace(".", "/"));
            if (destDeleteDir.exists()) {
                File[] filesToDelete = destDeleteDir.listFiles();
                for (File file : filesToDelete) {
                    file.delete();
                }
            }

            // scan for all *pcml files
            List<File> pcmlFiles = findPCMLFilesInDirectory(sourceDirectory);
            for (File file : pcmlFiles) {

                FileInputStream fis = new FileInputStream(file);

                SAXBuilder builder = new SAXBuilder();
                Document doc = builder.build(fis);

                Element rootNode = doc.getRootElement();
                List<Element> structs = rootNode.getChildren("struct");
                for (Element struct : structs) {
                    createJavaClass(struct, packageName, destDir);
                }
            }

        } catch (JClassAlreadyExistsException | IOException | JDOMException e) {
            throw new RuntimeException("Could not genereate JavaBeans from PCML-Files", e);
        }
    }

    private void createJavaClass(Element struct, String packageName, File destDir) throws JClassAlreadyExistsException,
            IOException {
        String structName = struct.getAttributeValue("name");

        String className = toCamelCase(structName, true);

        JCodeModel codeModel = new JCodeModel();
        JDefinedClass myClass = codeModel._class(packageName + "." + className);

        List<Element> children = struct.getChildren("data");
        for (Element dataField : children) {

            String nameRpg = dataField.getAttributeValue("name");

            Class<?> fieldType = mapToJavaType(dataField.getAttributeValue("type"),
                    dataField.getAttributeValue("length"), dataField.getAttributeValue("precision"));
            String name = toCamelCase(nameRpg);
            JFieldVar field = myClass.field(JMod.PRIVATE, fieldType, name);
            String capitalName = toCamelCase(name, true);
            String getterName = "get" + capitalName;
            String setterName = "set" + capitalName;
            JMethod getter = myClass.method(JMod.PUBLIC, fieldType, getterName);
            getter.body()._return(field);
            JMethod setter = myClass.method(JMod.PUBLIC, void.class, setterName);
            setter.param(fieldType, name);
            setter.body().assign(JExpr._this().ref(name), JExpr.ref(name));
        }

        codeModel.build(destDir);
    }

    /**
     * Finds the correct Java class for the given parameters
     * 
     * <table border=1>
     * <tr valign=top>
     * <th>PCML Description</th>
     * <th>Object Returned</th>
     * </tr>
     * <tr valign=top>
     * <td><code>type=char</td>
     * <td><code>String</code></td>
     * </tr>
     * <tr valign=top>
     * <td><code>type=byte</td>
     * <td><code>byte[]</code></td>
     * </tr>
     * <tr valign=top>
     * <td><code>type=int<br>
                             length=2<br>
                             precision=15</td>
     * <td><code>Short</code></td>
     * </tr>
     * <tr valign=top>
     * <td><code>type=int<br>
                             length=2<br>
                             precision=16</td>
     * <td><code>Integer</code></td>
     * </tr>
     * <tr valign=top>
     * <td><code>type=int<br>
                             length=4<br>
                             precision=31</td>
     * <td><code>Integer</code></td>
     * </tr>
     * <tr valign=top>
     * <td><code>type=int<br>
                             length=4<br>
                             precision=32</td>
     * <td><code>Long</code></td>
     * </tr>
     * <tr valign=top>
     * <td><code>type=int<br>
                             length=8<br>
                             precision=63</td>
     * <td><code>Long</code></td>
     * </tr>
     * <tr valign=top>
     * <td><code>type=int<br>
                             length=8<br>
                             precision=64</td>
     * <td><code>BigInteger</code></td>
     * </tr>
     * <tr valign=top>
     * <td><code>type=packed</td>
     * <td><code>BigDecimal</code></td>
     * </tr>
     * <tr valign=top>
     * <td><code>type=zoned</td>
     * <td><code>BigDecimal</code></td>
     * </tr>
     * <tr valign=top>
     * <td><code>type=float<br>
                             length=4</td>
     * <td><code>Float</code></td>
     * </tr>
     * <tr valign=top>
     * <td><code>type=float<br>
                             length=8</td>
     * <td><code>Double</code></td>
     * </tr>
     * <tr valign=top>
     * <td><code>type=date</td>
     * <td><code>java.sql.Date</code></td>
     * </tr>
     * <tr valign=top>
     * <td><code>type=time</td>
     * <td><code>java.sql.Time</code></td>
     * </tr>
     * <tr valign=top>
     * <td><code>type=timestamp</td>
     * <td><code>java.sql.Timestamp</code></td>
     * </tr>
     * </table>
     * 
     * @param attributeValue
     * @param attributeValue2
     * @return
     */
    private static Class<?> mapToJavaType(String type, String lengthString, String precisionString) {
        Integer length = lengthString != null && !lengthString.isEmpty() ? Integer.valueOf(lengthString) : 4;
        Integer precision = precisionString != null && !precisionString.isEmpty() ? Integer.valueOf(precisionString)
                : 32;
        switch (type) {
            case "char":
                return String.class;
            case "byte":
                return byte[].class;
            case "int":
                switch (length) {
                    case 2:
                        if (precision < 16) {
                            return Short.class;
                        } else {
                            return Integer.class;
                        }
                    case 4:
                        if (precision < 32) {
                            return Integer.class;
                        } else {
                            return Long.class;
                        }
                    case 8:
                        if (precision < 64) {
                            return Long.class;
                        } else {
                            return BigInteger.class;
                        }
                    default:
                        return Long.class;
                }
            case "packed":
                return BigDecimal.class;
            case "zoned":
                return BigDecimal.class;
            case "float":
                switch (length) {
                    case 4:
                        return Float.class;
                    case 8:
                        return Double.class;
                    default:
                        return Double.class;
                }
            case "date":
                return Date.class;
            case "time":
                return Time.class;
            case "timestamp":
                return Timestamp.class;
            default:
                return String.class;
        }
    }

    /**
     * Converts a string from UNDERSCORE_CASE to camelCase
     * 
     * @param structName
     * @return
     */
    public static String toCamelCase(String structName) {
        return toCamelCase(structName, false);
    }

    public static String toCamelCase(String structName, boolean firstLetterUppercase) {
        StringBuilder camelCase = new StringBuilder();
        for (int i = 0; i < structName.length(); i++) {
            if (firstLetterUppercase && i == 0) {
                camelCase.append(structName.charAt(0));
            } else {
                char c = structName.charAt(i);
                if ('_' == c && i + 1 < structName.length()) {
                    camelCase.append(structName.charAt(++i));
                } else {
                    camelCase.append(Character.toLowerCase(c));
                }
            }
        }
        return camelCase.toString();
    }

    /**
     * returns a list of all .pcml files in the classpath
     * 
     * @return
     */
    public List<File> findPCMLFilesInClasspath() {
        List<File> result = new LinkedList<>();

        URLClassLoader contextClassLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader();

        for (URL url : contextClassLoader.getURLs()) {
            File file = new File(url.getPath());
            result.addAll(handleFile(file));
        }

        return result;
    }

    public List<File> findPCMLFilesInDirectory(String directory) {
        return handleDirectory(new File(directory));
    }

    private List<File> handleFile(File file) {
        List<File> result = new LinkedList<>();
        if (file.isDirectory()) {
            result.addAll(handleDirectory(file));
        } else if (file.isFile() && file.getName().endsWith(".pcml")) {
            result.add(file);
        }
        return result;
    }

    private List<File> handleDirectory(final File dir) {
        List<File> result = new LinkedList<>();
        for (File file : dir.listFiles()) {
            result.addAll(handleFile(file));
        }
        return result;
    }
}
