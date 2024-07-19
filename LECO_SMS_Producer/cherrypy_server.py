import os
import sys
import cherrypy
from django.core.wsgi import get_wsgi_application
import signal

# Set the Django settings module
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'rabbitmq_project.settings')

# Add the project directory to the sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), 'rabbitmq_project')))

# Create the WSGI application
application = get_wsgi_application()

class DjangoApp(object):
    @cherrypy.expose
    def default(self, *args, **kwargs):
        # Delegate the request to the WSGI application
        return cherrypy.request.wsgi_environ['wsgi.input']

# Mount the WSGI application at the root URL
cherrypy.tree.graft(application, '/')

# Remove the default CherryPy server
cherrypy.server.unsubscribe()

# Instantiate a new CherryPy server object
server = cherrypy._cpserver.Server()

# Configure the server object
server.socket_host = '0.0.0.0'
server.socket_port = 8000
server.thread_pool = 30

# For SSL support, uncomment the following lines:
# server.ssl_module = 'builtin'
# server.ssl_certificate = '/path/to/ssl/certificate.crt'
# server.ssl_private_key = '/path/to/ssl/private.key'

# Subscribe this server
server.subscribe()

def signal_handler(signum, frame):
    # Stop the CherryPy engine
    cherrypy.engine.exit()
    # Additional cleanup can be done here if necessary
    sys.exit(0)

# Register the signal handler for SIGINT (Ctrl+C)
signal.signal(signal.SIGINT, signal_handler)

# Start the CherryPy server engine
cherrypy.engine.start()
cherrypy.engine.block()
