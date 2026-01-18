import { useState, useEffect } from 'react';
import axios from 'axios';
import {
    Container, Typography, Card, CardContent, Slider, Button,
    List, ListItem, ListItemText, Divider, Chip, TextField, Box
} from '@mui/material';

const API_URL = '/api';

function App() {
    // Состояния приложения
    const [view, setView] = useState('loading');
    const [user, setUser] = useState(null);

    const [username, setUsername] = useState('');
    const [first_name, setFirstName] = useState('');

    // Данные
    const [projects, setProjects] = useState([]);
    const [history, setHistory] = useState([]);

    // Формы
    const [newProjectTitle, setNewProjectTitle] = useState('');
    const [newProjectDesc, setNewProjectDesc] = useState('');

    // Голосование
    const [selectedProject, setSelectedProject] = useState(null);
    const [voteCount, setVoteCount] = useState(1);

    useEffect(() => {
        const initApp = async () => {
            const tgUser = window.Telegram.WebApp.initDataUnsafe?.user;

            if (tgUser) {
                const displayName = tgUser.username ? `@${tgUser.username}` : tgUser.first_name;
                setUsername(displayName);
                setFirstName(tgUser.first_name)

                try {
                    const res = await axios.post(`${API_URL}/auth/login/${tgUser.id}`);
                    setUser(res.data);
                    setView('role-select');
                } catch (e) {
                    alert("Ошибка логина: " + e.message);
                }
            } else {
                // Fallback для браузера
                setUsername("Test User");
                const res = await axios.post(`${API_URL}/auth/login/101`);
                setUser(res.data);
                setView('role-select');
            }
        };

        initApp();
    }, []);

    const loadProjects = async () => {
        const res = await axios.get(`${API_URL}/projects`);
        setProjects(res.data);
    };

    const loadHistory = async () => {
        const res = await axios.get(`${API_URL}/history/${user.userId}`);
        setHistory(res.data);
    };


    if (view === 'loading') return <Typography align="center" mt={5}>Загрузка...</Typography>;

    if (view === 'role-select') {
        return (
            <Container maxWidth="sm" style={{ marginTop: '50px', textAlign: 'center' }}>

                <Typography variant="h4" gutterBottom>
                    Привет, {first_name}! 👋
                </Typography>

                <Typography variant="caption" display="block" color="textSecondary">
                    ID: {user?.userId}
                </Typography>

                <Typography variant="body1" gutterBottom style={{marginTop: 10}}>
                    Твой баланс: <b>{user?.balance} QV</b>
                </Typography>

                <Box mt={4} display="flex" flexDirection="column" gap={2}>
                    <Button variant="contained" size="large" onClick={() => { setView('creator'); }}>
                        📝 Создать Опрос
                    </Button>
                    <Button variant="outlined" size="large" onClick={() => { setView('voter'); loadProjects(); }}>
                        🗳️ Голосовать
                    </Button>
                </Box>
            </Container>
        );
    }

    if (view === 'creator') {
        const createProject = async () => {
            await axios.post(`${API_URL}/projects`, {
                title: newProjectTitle,
                description: newProjectDesc,
                creatorId: user.userId
            });
            alert('Проект создан!');
            setNewProjectTitle('');
            setNewProjectDesc('');
            setView('role-select');
        };

        return (
            <Container maxWidth="sm" style={{ marginTop: '20px' }}>
                <Button onClick={() => setView('role-select')}>← Назад</Button>
                <Typography variant="h5" mt={2} mb={2}>Создание Опроса</Typography>

                <Card>
                    <CardContent>
                        <TextField fullWidth label="Название" value={newProjectTitle} onChange={e => setNewProjectTitle(e.target.value)} margin="normal"/>
                        <TextField fullWidth label="Описание" value={newProjectDesc} onChange={e => setNewProjectDesc(e.target.value)} margin="normal" multiline rows={3}/>
                        <Button variant="contained" fullWidth style={{marginTop: 20}} onClick={createProject}>Создать</Button>
                    </CardContent>
                </Card>
            </Container>
        );
    }

    if (view === 'voter') {
        const sendVote = async () => {
            if (!selectedProject) return;
            try {
                await axios.post(`${API_URL}/votes`, {
                    userId: user.userId,
                    projectId: selectedProject.id,
                    voteCount: voteCount
                });
                alert('Голос отправлен!');
            } catch(e) {
                alert('Ошибка!');
            }
        };

        return (
            <Container maxWidth="sm" style={{ marginTop: '20px' }}>
                <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                    <Button onClick={() => setView('role-select')}>← Меню</Button>
                    <Button onClick={() => { setView('history'); loadHistory(); }}>История 🕒</Button>
                </div>

                <Typography variant="h6" align="center" mt={2}>Проекты</Typography>

                <List>
                    {projects.map(p => (
                        <Card key={p.id} style={{marginBottom: 10, border: selectedProject?.id === p.id ? '2px solid #1976d2' : 'none'}} onClick={() => setSelectedProject(p)}>
                            <CardContent>
                                <Typography variant="h6">{p.title}</Typography>
                                <Typography variant="body2" color="textSecondary">{p.description}</Typography>
                            </CardContent>
                        </Card>
                    ))}
                </List>

                {selectedProject && (
                    <div style={{position: 'fixed', bottom: 0, left: 0, right: 0, background: 'white', padding: 20, boxShadow: '0 -2px 10px rgba(0,0,0,0.1)'}}>
                        <Typography>Голосов за "{selectedProject.title}": {voteCount}</Typography>
                        <Slider value={voteCount} min={1} max={10} onChange={(e, v) => setVoteCount(v)} valueLabelDisplay="auto" />
                        <Typography align="right" color="error">Цена: {voteCount * voteCount} QV</Typography>
                        <Button variant="contained" fullWidth onClick={sendVote}>Подтвердить</Button>
                    </div>
                )}
            </Container>
        );
    }

    // Экран Истории
    if (view === 'history') {
        return (
            <Container maxWidth="sm" style={{ marginTop: '20px' }}>
                <Button onClick={() => setView('voter')}>← К проектам</Button>
                <Typography variant="h5" mt={2}>Моя История</Typography>
                <List>
                    {history.map((h, i) => (
                        <ListItem key={i} divider>
                            <ListItemText
                                primary={`Проект ID: ${h.projectId}`}
                                secondary={`Потрачено: ${h.cost} QV • Tx: ${h.txHash.substring(0, 10)}...`}
                            />
                        </ListItem>
                    ))}
                </List>
            </Container>
        );
    }
}

export default App;