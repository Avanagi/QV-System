import { useState, useEffect } from 'react';
import axios from 'axios';
import { Container, Typography, Card, CardContent, Slider, Button, List, ListItem, ListItemText, Divider, Chip } from '@mui/material';

// Адрес нашего Gateway
const API_URL = '/api';
// Хардкодим юзера для демо (в реальности берем из Telegram initData)
const USER_ID = 101;

function App() {
    const [activeTab, setActiveTab] = useState('vote'); // 'vote' или 'history'
    const [history, setHistory] = useState([]);

    // Данные для голосования
    const [projectId, setProjectId] = useState(1);
    const [voteCount, setVoteCount] = useState(1);

    // Загрузка истории
    const loadHistory = async () => {
        try {
            const res = await axios.get(`${API_URL}/history/${USER_ID}`);
            setHistory(res.data);
        } catch (e) {
            console.error("Ошибка загрузки истории", e);
        }
    };

    useEffect(() => {
        if (activeTab === 'history') loadHistory();
    }, [activeTab]);

    // Отправка голоса
    const handleVote = async () => {
        try {
            await axios.post(`${API_URL}/votes`, {
                userId: USER_ID,
                projectId: projectId,
                voteCount: voteCount
            });
            alert(`Голос отправлен! Стоимость: ${voteCount * voteCount}`);
            setVoteCount(1); // Сброс
        } catch (e) {
            alert('Ошибка голосования (проверь консоль)');
        }
    };

    return (
        <Container maxWidth="sm" style={{ marginTop: '20px' }}>
            <Typography variant="h4" gutterBottom align="center">
                QV Voting App
            </Typography>

            {/* Меню переключения */}
            <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '20px', gap: '10px' }}>
                <Button variant={activeTab === 'vote' ? "contained" : "outlined"} onClick={() => setActiveTab('vote')}>
                    Голосовать
                </Button>
                <Button variant={activeTab === 'history' ? "contained" : "outlined"} onClick={() => setActiveTab('history')}>
                    История
                </Button>
            </div>

            {/* ЭКРАН ГОЛОСОВАНИЯ */}
            {activeTab === 'vote' && (
                <Card>
                    <CardContent>
                        <Typography variant="h6">Выберите проект (ID: {projectId})</Typography>
                        <Slider
                            value={projectId}
                            min={1} max={10}
                            onChange={(e, val) => setProjectId(val)}
                            marks
                            valueLabelDisplay="auto"
                        />

                        <Typography variant="h6" style={{marginTop: 20}}>Количество голосов: {voteCount}</Typography>
                        <Slider
                            value={voteCount}
                            min={1} max={10}
                            onChange={(e, val) => setVoteCount(val)}
                            valueLabelDisplay="auto"
                        />

                        <Typography color="error" align="right" variant="h5" style={{margin: '20px 0'}}>
                            Стоимость: {voteCount * voteCount} кредитов
                        </Typography>

                        <Button variant="contained" color="primary" fullWidth onClick={handleVote}>
                            Отправить голос
                        </Button>
                    </CardContent>
                </Card>
            )}

            {/* ЭКРАН ИСТОРИИ */}
            {activeTab === 'history' && (
                <List style={{ backgroundColor: '#fff', borderRadius: '8px' }}>
                    {history.length === 0 ? <Typography align="center" style={{padding: 20}}>Истории нет</Typography> : null}

                    {history.map((item, index) => (
                        <div key={index}>
                            <ListItem>
                                <ListItemText
                                    primary={`Проект #${item.projectId} • Голосов: ${item.voteCount}`}
                                    secondary={
                                        <>
                                            <span>Списано: {item.cost}</span> <br/>
                                            <span style={{fontSize: '0.8em', color: 'gray'}}>{item.timestamp}</span>
                                        </>
                                    }
                                />
                                <Chip label="Confirmed" color="success" size="small" />
                            </ListItem>
                            <Divider />
                        </div>
                    ))}
                </List>
            )}
        </Container>
    );
}

export default App;