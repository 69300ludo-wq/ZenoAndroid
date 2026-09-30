from pathlib import Path

p = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')
s = p.read_text(encoding='utf-8')

bottom_nav = '''                    NavigationBar(containerColor = Color(0xF006122A)) {
                        NavItem(Icons.Default.Home, "Accueil", false) { screen = Screen.HOME }
                        NavItem(Icons.Default.Chat, "Chat", screen == Screen.CHAT) { screen = Screen.CHAT }
                        NavItem(Icons.Default.Apps, "Apps", screen == Screen.APPS) { screen = Screen.APPS }
                        NavItem(Icons.Default.Groups, "Membres", screen == Screen.COMMUNITY) { screen = Screen.COMMUNITY }
                        NavItem(Icons.Default.Settings, "Réglages", screen == Screen.SETTINGS) { screen = Screen.SETTINGS }
                    }
'''
if bottom_nav in s:
    s = s.replace(bottom_nav, '', 1)

old_actions = '''        HomeAction(Icons.Default.Palette, "Personnalisation", "Thème et apparence", Screen.CUSTOMIZE, Color(0xFFFFB52E)),
        HomeAction(Icons.Default.Groups, "Communauté", "Partage et découvre", Screen.COMMUNITY, Color(0xFF4589FF))
'''
new_actions = '''        HomeAction(Icons.Default.Palette, "Personnalisation", "Thème et apparence", Screen.CUSTOMIZE, Color(0xFFFFB52E)),
        HomeAction(Icons.Default.Groups, "Communauté", "Partage et découvre", Screen.COMMUNITY, Color(0xFF4589FF)),
        HomeAction(Icons.Default.Mic, "Phrase vocale", "Choisis la phrase pour réveiller Zeno", Screen.SETTINGS, Color(0xFFFF4FC4)),
        HomeAction(Icons.Default.Settings, "Paramètres", "Réglages de Zeno", Screen.SETTINGS, Color(0xFF59D8FF)),
        HomeAction(Icons.Default.Info, "À propos", "Version et informations", Screen.ABOUT, Color(0xFF7D7CFF))
'''
if old_actions in s:
    s = s.replace(old_actions, new_actions, 1)

p.write_text(s, encoding='utf-8')
print('Menu bas retiré; tous les modules et la phrase vocale sont sur l accueil')
