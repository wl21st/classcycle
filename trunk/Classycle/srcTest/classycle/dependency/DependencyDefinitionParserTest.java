/*
 * Created on 30.08.2004
 */
package classycle.dependency;

import java.util.ArrayList;
import java.util.HashMap;

import junit.framework.TestCase;

/**
 * @author  Franz-Josef Elmer
 */
public class DependencyDefinitionParserTest extends TestCase
{
  
  public void testShowStatements() 
  {
    check("show a b c", new String[] {"[a, b, c]"});
    check("show a\n show b c", new String[] {"[a]", "[b, c]"});
  }
  
  public void testDependencyStatements() 
  {
    check("check java.lang.* independentOf java.awt.*", 
          new String[] {"check java.lang.* independentOf java.awt.*"});
    check("[lang] = java.lang.*\n"
          + "show shortestPathsOnly\n"
          + "check [lang] independentOf java.awt.*",  
          new String[] {"[shortestPathsOnly]",
                        "check [lang] independentOf java.awt.*"});
    check("[lang] = java.lang.*\n"
          + "  check   [lang]   java.util.*   independentOf java.awt.*",  
          new String[] {"check [lang] java.util.* independentOf java.awt.*"});
    check("[lang] = java.lang.*\n"
          + "  check   [lang]   java.util.*   independentOf\\\njava.awt.*",  
          new String[] {"check [lang] java.util.* independentOf java.awt.*"});
    check("######\n"
          + "[lang] = java.lang.*\n"
          + "# this is a comment\n"
          + "    # and this too\n\r\n"
          + "check [lang] java.util.* independentOf \\ \n  java.awt.*\n"  
          + "check java.util.* independentOf \\\n   javax.swing.* java.awt.*\n",  
          new String[] {"check [lang] java.util.* independentOf java.awt.*", 
                 "check java.util.* independentOf javax.swing.* java.awt.*"});
    check("[lang] = java.lang.*\n"
          + "check [lang] java.util.* independentOf\\  \n# hello\njava.awt.*",  
          new String[] {"check [lang] java.util.* independentOf java.awt.*"});
    check("[lang] = java.lang.*\n"
          + "[a] = [lang] excluding java.lang.Class\n"
          + "check [a] independentOf java.awt.*",  
          new String[] {"check [a] independentOf java.awt.*"});
  }
  
  public void testLayeringStatements() 
  {
    check("layer a = java.lang.*\n"
          + "layer b = java.util.*\n"
          + "check layeringOf a b", new String[] {"check layeringOf a b"});
    check("layer a = java.lang.*\n"
          + "layer b = java.util.*\n"
          + "check strictLayeringOf a b", 
          new String[] {"check strictLayeringOf a b"});
  }
  
  private void check(String definition, String[] expectedStatements) 
  {
    DependencyDefinitionParser parser 
        = new DependencyDefinitionParser(definition, new MockResultRenderer());
    Statement[] statements = parser.getStatements();
    int len = Math.min(statements.length, expectedStatements.length);
    for (int i = 0; i < len; i++)
    {
      assertEquals("Statement " + (i + 1), expectedStatements[i], 
                   statements[i].toString());
    }
    if (len < statements.length)
    {
      fail(statements.length - len + " additional statements");
    }
    if (len < expectedStatements.length)
    {
      fail(expectedStatements.length - len + " missing statements");
    }
  }
  
  private void check(String definition, String statementCodes) 
  {
    DependencyDefinitionParser parser 
        = new DependencyDefinitionParser(definition, new MockResultRenderer());
    Statement[] statements = parser.getStatements();
    int len = Math.min(statements.length, statementCodes.length());
    for (int i = 0; i < len; i++)
    {
      String className = statements[i].getClass().getName();
      int index = className.lastIndexOf('.');
      assertEquals("Statement " + (i + 1), statementCodes.charAt(i), 
                   Character.toLowerCase(className.charAt(index + 1)));
    }
    if (len < statements.length)
    {
      fail(statements.length - len + " additional statements");
    }
    if (len < statementCodes.length())
    {
      fail(statementCodes.length() - len + " missing statements");
    }
  }
  
  private static class MockPreference implements Preference
  {
    String _preference;
    public MockPreference(String preference)
    {
      _preference = preference;
    }
    public String getKey()
    {
      return _preference;
    }
    public String toString()
    {
      return _preference;
    }
  }
  
  private static class MockPreferenceFactory implements PreferenceFactory
  {
    private final HashMap _keyToPreferenceMap = new HashMap();
    public Preference get(String key)
    {
      Preference preference = (Preference) _keyToPreferenceMap.get(key);
      if (preference == null)
      {
        preference = new MockPreference(key);
        _keyToPreferenceMap.put(key, preference);
      }
      return preference;
    }
  }
  
  private static class MockResultRenderer implements ResultRenderer
  {
    ArrayList list = new ArrayList();
    boolean shortestPathsOnly;
    private PreferenceFactory _factory = new MockPreferenceFactory();
    public void considerPreference(Preference preference)
    {
      list.add(preference);
    }
    public Result getDescriptionOfCurrentPreferences()
    {
      return new MockResult(list.toString());
    }
    public PreferenceFactory getPreferenceFactory()
    {
      return _factory;
    }
    public boolean onlyShortestPaths()
    {
      return shortestPathsOnly;
    }
    public String render(Result result)
    {
      return null;
    }
  }
  
  private static class MockResult implements Result
  {
    String value;
    public MockResult(String value)
    {
      this.value = value;
    }
  }
}
