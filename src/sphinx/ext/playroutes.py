from pygments.lexer import RegexLexer, bygroups
from pygments.token import *

class PlayRoutesLexer(RegexLexer):

  name = 'ROUTES'
  aliases = ['routes']
  filenames = ['routes', '*.routes']

  tokens = {
      'root': [
          (r'\s+', Text),
          (r'#.*?$', Comment),
          (r'(GET|POST|PATCH|DELETE|PUT|OPTIONS|->)\s+', Keyword, 'url')
      ],
      'url': [
          (r'\s+', Text, 'object'),
          (r'[^:]', Literal.String),
          (r':\w+', Name.Variable),
      ],
      'object': [
          (r'(\w+\.)+', Name.Class),
          (r'\w+', Name.Function),
          (r'(\()([^)]+)(\))', bygroups(Text, Name.Variable, Text)),
      ]
  }
  
  
    
def setup(app):
  app.add_lexer('routes', PlayRoutesLexer())


